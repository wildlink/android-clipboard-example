package com.example.myWildlinkApp

import android.app.Service
import android.content.*
import android.os.IBinder
import android.util.Log
import me.wildfire.apiwrapper.ApiWrapper
import me.wildfire.apiwrapper.ApiWrapperException
import me.wildfire.apiwrapper.public_models.Concept
import me.wildfire.apiwrapper.public_models.Concepts
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.net.URI
import java.net.URISyntaxException

class ClipboardMonitorService : Service() {

    private val listener = ClipboardManager.OnPrimaryClipChangedListener { performClipboardCheck() }

    override fun onCreate() {
        Log.d("exampleapp", "clipboard monitor service onCreate called")

        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).addPrimaryClipChangedListener(listener)
        var concepts: Concepts? = null

        // fetch the Wildfire domains and store them in prefs so we can consult them as a whitelist
        doAsync {
            try {
                do {
                    val nextCursor = concepts?.nextCursor
                    if (nextCursor != null) {
                        // add the new page of concepts to our flat array of all concepts
                        concepts?.concepts?.forEach{
                            allConcepts.add(it)
                        }

                        Log.d("exampleapp", "Concept count: ".plus(allConcepts.count()))

                        concepts = ApiWrapper.getConcept(kind = "domain", cursor = nextCursor )
                    } else {
                        // we're on the last page of concepts now
                        concepts = ApiWrapper.getConcept(kind = "domain")
                    }
                } while (concepts?.nextCursor != null)

            } catch (e: ApiWrapperException) {
                Log.d("exampleapp", "Error ".plus(e.statusCode).plus(" ").plus(e.message))
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @Throws(URISyntaxException::class)
    fun getDomainName(url: String): String {
        val uri = URI(url)
        val domain = uri.getHost()
        return if (domain.startsWith("www.")) domain.substring(4) else domain
    }

    private fun performClipboardCheck() {
        Log.d("exampleapp", "checking clipboard ... ")

        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (cb.hasPrimaryClip()) {
            val cd = cb.primaryClip
            if (cd!!.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                val clipboard = cd.getItemAt(0).text.toString()

                // is the copied text a URL?
                if (clipboard.startsWith("http")){
                    val copiedDomain = getDomainName(clipboard)
                    Log.d("exampleapp", clipboard + " -- domain = " + copiedDomain)

                    if (copiedDomain == "wild.link"){
                        Log.d("exampleapp", "clipboard is already a wild.link, so stop eval")
                        return
                    }

                    // check to see if the copied URL matches a domain in our partner merchants
                    for (i in allConcepts){
                        if (clipboard.contains(i.Value)) {
                            Log.d("exampleapp", "MATCHED!!! - " + i.Value)

                            // create the wild.link vanity URL
                            doAsync {
                                val vanity = try {
                                    Log.d("exampleapp", "creating the vanity URL for " + clipboard)
                                    ApiWrapper.createVanity(clipboard)
                                } catch (e: ApiWrapperException) {
                                    null
                                }

                                uiThread {
                                    vanity?.vanityUrl?.let {
                                        // replace user clipboard with the newly created wild.link
                                        Log.d("exampleapp", "wild.link created : " + vanity.vanityUrl)
                                        val clip: ClipData = ClipData.newPlainText("wild.link", vanity.vanityUrl)
                                        cb.primaryClip = clip
                                    }
                                }
                            }

                            // we found our matching domain and are creating a wild.link so stop the domain-matching loop
                            break
                        }
                    }
                } else {
                    Log.d("exampleapp", "copied text is not a URL")
                }
            }
        }
    }

    companion object {
        val allConcepts:MutableList<Concept> = mutableListOf()
    }
}