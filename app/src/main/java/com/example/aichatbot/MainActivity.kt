package com.example.aichatbot

import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.noties.markwon.Markwon
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.Objects

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btSend: CardView

    private lateinit var idIVMic: CardView

    private lateinit var adapter: ChatAdapter

    private val REQUEST_CODE_SPEECH_INPUT = 1

    private val chatList = mutableListOf<ChatModel>()
//    private lateinit var tvChat: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        etMessage = findViewById(R.id.etMessage)
        btSend = findViewById(R.id.btSend)
        idIVMic = findViewById(R.id.idIVMic)

        setUPRecyclerView()

        btSend.setOnClickListener {
            if (etMessage.text.toString().isEmpty()) {
                Toast.makeText(this, "Please enter your question", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else {
                val message = etMessage.text.toString()

                addMessage(ChatModel(message, true))

                etMessage.text.clear()
                apiCall(message)
            }
        }
        idIVMic.setOnClickListener {
            // on below line we are calling speech recognizer intent.
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            // on below line we are passing language model
            // and model free form in our intent
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            // on below line we are passing our
            // language as a default language.
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )

            // on below line we are specifying a prompt
            // message as speak to text on below line.
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Your Ai")

            // on below line we are specifying a try catch block.
            // in this block we are calling a start activity
            // for result method and passing our result code.
            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
            } catch (e: Exception) {
                // on below line we are displaying error message in toast
                Toast
                    .makeText(
                        this@MainActivity, " " + e.message,
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // in this method we are checking request
        // code with our result code.
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            // on below line we are checking if result code is ok
            if (resultCode == RESULT_OK && data != null) {

                // in that case we are extracting the
                // data from our array list
                val res: ArrayList<String> =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                // on below line we are setting data
                // to our output text view.

                addMessage(ChatModel(Objects.requireNonNull(res)[0], true))

                etMessage.text.clear()
                apiCall(Objects.requireNonNull(res)[0])
            }
        }
    }

    fun addMessage(message: ChatModel) {
        chatList.add(message)

        adapter.notifyItemInserted(chatList.size - 1)

        recyclerView.scrollToPosition(chatList.size - 1)
    }

    private fun setUPRecyclerView() {
        adapter = ChatAdapter(chatList, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun apiCall(prompt: String) {
        val url = "https://api.groq.com/openai/v1/chat/completions"

        //========="YOUR_API_KEY"========================================================

        val GROQ_API_KEY = "YOUR_API_KEY"

        //========================================================================================

        val jsonBody = JSONObject().apply{
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("model", "qwen/qwen3.8-27b")
            put("temperature", 0.6)
            put("max_completion_tokens", 2048)
            put("top_p",  0.95)
            put("stream", false)
            put("reasoning_effort", "default")
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            url,
            jsonBody,
            Response.Listener{ response ->
                try {
                    val answer = response
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    addMessage(ChatModel(answer, false))

//                    val markwon = Markwon.create(this)
//                    markwon.setMarkdown(tvChat, answer)
                }
                catch (e : Exception){
                    addMessage(ChatModel("Error", false))
                }

            },
            Response.ErrorListener{ error ->
                addMessage(ChatModel("Error", false))
            }){

            override fun getHeaders(): MutableMap<String?, String?>? {
                return hashMapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer $GROQ_API_KEY"
                )

            }
        }

        request.retryPolicy = DefaultRetryPolicy(
            60000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(this).add(request)

    }
}