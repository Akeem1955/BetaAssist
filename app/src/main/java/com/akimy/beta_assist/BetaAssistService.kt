package com.akimy.beta_assist

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import org.vosk.android.StorageService
import java.io.IOException
import java.util.UUID

class BetaAssistService: AccessibilityService(), org.vosk.android.RecognitionListener {
    private var job: Job? =null
    private var llmInference: LlmInference? = null
    private var packageManager: PackageManager? = null
    private var vModel: Model? = null
    private var enabled = false
    private var speechService: SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null
    private var tts: TextToSpeech? = null
    private var ttsListenerSucess = false
    private var stt: SpeechRecognizer? = null
    private val SttRecognition  = object  : RecognitionListener {
        override fun onBeginningOfSpeech() {
            Log.d("BetaAssistService", "onBeginningOfSpeech")
        }

        override fun onBufferReceived(p0: ByteArray?) {
            Log.d("BetaAssistService", "onBufferReceived")
        }

        override fun onEndOfSpeech() {
            Log.d("BetaAssistService", "onEndOfSpeech")
            //speechService?.startListening(this@BetaAssistService)
        }

        override fun onError(p0: Int) {
            Log.d("BetaAssistService", "onError")
            //speechService?.startListening(this@BetaAssistService)
            println("Error is ${p0 == SpeechRecognizer.ERROR_NETWORK}")
            removeLoading()

        }

        override fun onEvent(p0: Int, p1: Bundle?) {
            Log.d("BetaAssistService", "onEvent")
        }

        override fun onPartialResults(p0: Bundle?) {
            Log.d("BetaAssistService", "onPartialResults")
        }

        override fun onReadyForSpeech(p0: Bundle?) {
            Log.d("BetaAssistService", "onReadyForSpeech")
        }

        override fun onResults(p0: Bundle?) {
            removeLoading()
            Log.d("BetaAssistService", "onResults ---> ${p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.toString()}")
            val results = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.getOrNull(0)?:""
            modelNlp(results)
            //processCommand(results)
            //initializeModel(this@BetaAssistService,results)


        }

        override fun onRmsChanged(p0: Float) {
            Log.d("BetaAssistService", "onRmsChanged")
        }
    }
    private var layout:FrameLayout?=null
    private var layoutB:FrameLayout?=null
    private var layoutC:FrameLayout?=null
    private var windManager: WindowManager?=null
    private var session: LlmInferenceSession?=null


    private val scope = CoroutineScope(Dispatchers.Default)
    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
        //println("Implemented ")
    }
    override fun onInterrupt() {
        println("BetaAssist Service Interrupted")
        //stt?.stopListening()
    }
    override fun onServiceConnected() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted - can't proceed with voice functionality
            Log.e("BetaAssistService", "RECORD_AUDIO permission not granted")
            readTextAloud("Audio recording permission is required for voice commands.")
            // Optional: Send a broadcast to your main activity to request permission
            val intent = Intent(this,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            disableSelf()
            return
        }
        initializeModel(this)
        initializeImageModel(this)
        packageManager = this.applicationContext.packageManager
        initVosk(this)
        tts = TextToSpeech(this){status->
            ttsListenerSucess = status == TextToSpeech.SUCCESS
            if (ttsListenerSucess)welcomeUser() else stopSelf()

        }
        stt = SpeechRecognizer.createSpeechRecognizer(this)
        stt?.setRecognitionListener(SttRecognition)


    }




    // private functions
    fun showLoading(){
        if(windManager == null)windManager =  getSystemService(WINDOW_SERVICE) as WindowManager
        layout = FrameLayout(this)

        val lp = WindowManager.LayoutParams()
        lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        lp.format = PixelFormat.TRANSLUCENT

        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT

        lp.gravity = Gravity.CENTER

        val inflater = LayoutInflater.from(this)
        inflater.inflate(R.layout.waves, layout)
        windManager?.addView(layout, lp)
    }
    fun removeLoading(){
        if (layout != null && windManager != null){
            windManager?.removeView(layout)
            layout = null
        }
    }
    fun showProcessing(){
        if(windManager == null)windManager =  getSystemService(WINDOW_SERVICE) as WindowManager
        layoutB = FrameLayout(this)

        val lp = WindowManager.LayoutParams()
        lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        lp.format = PixelFormat.TRANSLUCENT

        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT

        lp.gravity = Gravity.CENTER

        val inflater = LayoutInflater.from(this)
        inflater.inflate(R.layout.load, layoutB)
        windManager?.addView(layoutB, lp)
    }
    fun removeProcessing(){
        if (layoutB != null && windManager != null){
            windManager?.removeView(layoutB)
            layoutB = null
        }
    }
    private fun welcomeUser(){

        val msg = "Hi, My Name is Beta Assist. i Am an Accessibility Service, To Help You Make Your Device more Powerful!. Powered By Gemma 3n. To Call Me Say Gemma"
        readTextAloud(msg)
        scope.launch {
            delay(10000)
            withContext(Dispatchers.Main){
                Log.d("BetaAssistService", "Starting Listening")
                recognizeMicrophone()
            }
        }

    }
    private fun recognizeMicrophone() {
        if (!enabled)return
        if (speechService != null) {
            speechService?.stop()
            speechService = null
        } else {
            try {
                val rec = Recognizer(vModel, 16000.0f)
                speechService = SpeechService(rec, 16000.0f)
                speechService?.startListening(this)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    private fun initVosk(ctx: Context){
        println("about to initialize the vosk library")
        StorageService.unpack(
            ctx, "vosk_model_small_en_us", "model",
            { model: Model? ->
                vModel = model
                enabled= true

            },
            {
                it.printStackTrace()
                println("----------------------------")
                println(it.message)
                println("-----------------------------")
                println("error happened so no model for u ....")
            })
    }
    private fun readTextAloud(text: String){
        val bundle = Bundle().apply {
            // Optional parameters
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) // full volume
            putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, 0.0f)     // center pan
            // You can also add stream info
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        tts?.speak(text, TextToSpeech.QUEUE_ADD,bundle, UUID.randomUUID().toString())
    }
    fun initializeModel(context: Context){
        if(llmInference != null)return
        // Set the configuration options for the LLM Inference task
        //val refinedPrompt = "$fewshot Convert this command to JSON: $prompt"

        println("Model About to Crank up the system...")
        val taskOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath("/data/local/tmp/llm/model_version.task")
            .setMaxTopK(64)
            .build()


// Create an instance of the LLM Inference task
        llmInference = LlmInference.createFromOptions(context, taskOptions)
        //val result = llmInference.generateResponse(refinedPrompt)
        //Log.d("BetaAssistService", "Model Response ---> $result")
        //processCommand(result.replace("```json","").replace("```","").trim())
    }
    fun modelNlp(instruction:String) {
        scope.launch {
            withContext(Dispatchers.Main){
                showProcessing()
            }
            val refinedInstruction = "$fewshot Convert this command to JSON: $instruction"
            val nlpOutputJson =llmInference?.generateResponse(refinedInstruction)
            println("Model Response ---> $nlpOutputJson")
            if (nlpOutputJson != null){
                processCommand(nlpOutputJson.replace("```json","").replace("```","").trim())
                println("Command Processed finished")
            }
            withContext(Dispatchers.Main){
                removeProcessing()
            }
        }
    }
    fun processCommand(json:String){
        println("Processing Command")
        println(json)
        try {
            val command = Json.decodeFromString<Command>(json)
            val action = try {
                ActionType.valueOf(command.function)
            }catch (_:IllegalArgumentException){ActionType.Nothing}
            when(action){
                ActionType.openApp -> {
                    val appName =command.appName
                    if(appName != null){
                        openApp(appName)
                    }else{
                        readTextAloud("Sorry I did not get The App Name.")
                    }
                }
                ActionType.extractText -> {
                    extractText()
                }
                ActionType.translateText -> {
                    translateText()
                }
                ActionType.summarizeTextOnscreen -> {
                    summarizeTextOnscreen()
                }
                ActionType.conversation -> {
                    if(command.prompt != null){
                        println("now calling conversation")
                        conversation(command.prompt!!)
                    }else{
                        readTextAloud("I did Not Get What You Said.")
                    }
                }
                ActionType.describeScreen -> {
                    if(command.prompt != null){
                        describeScreen(command.prompt!!)
                    }else{
                        readTextAloud("I did Not Get What You Said.")
                    }
                }
                ActionType.Nothing -> {
                    readTextAloud("Sorry i May not be able to do That.")
                }

                ActionType.rightSwipeGesture -> {
                    rightSwipeGesture()
                }
                ActionType.leftSwipeGesture -> {
                    leftSwipeGesture()
                }
                ActionType.upSwipeGesture -> {
                    upSwipeGesture()
                }
                ActionType.downSwipeGesture -> {
                    downSwipeGesture()
                }
            }

        }catch (e: Exception){
            readTextAloud("Sorry Error Happened When Processing")
            e.printStackTrace()
            println("Error ti shele")
            println(e.message)
        }
    }
    private fun openApp(appName: String): Boolean{

        val applications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager?.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        }
        else{
            packageManager?.getInstalledApplications(0)
        }

        for (i in 0..<(applications?.size?:0)){
            val temp = applications?.get(i)
            if(temp != null){
                println("temp is not null")
                val name = packageManager?.getApplicationLabel(temp)?:""
                if(name.isEmpty())return false
                if(name.contains(appName,true)){
                    println("About to launch the application")
                    println("${temp.packageName}: --> $name : ---> $appName")
                    val launchIntent = packageManager?.getLaunchIntentForPackage(temp.packageName)
                    startActivity(launchIntent)
                    return true
                }

            }
        }
        return false
    }
    private fun describeScreen(prompt: String){
        processImage(prompt)
    }
    private fun extractText(){
        scope.launch {
            if(rootInActiveWindow == null)return@launch
            val deque: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
            deque.add(rootInActiveWindow)
            while (deque.isNotEmpty()){
                val node = deque.removeFirst()
                if(node.text != null && node.text.isNotBlank()){
                    readTextAloud("${node.text}")
                    delay(2000)

                }
                println("<----- Next Text --->")
                for(i in 0..<node.childCount){
                    val child = node.getChild(i)
                    if(child != null)deque.add(child)
                }
            }
        }
    }
    private fun translateText(){
        val textOnScreen = buildString {
            if (rootInActiveWindow == null)return
            val deque: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
            deque.add(rootInActiveWindow)
            while (deque.isNotEmpty()){
                val node = deque.removeFirst()
                if(node.text != null && node.text.isNotBlank()){
                    append("${node.text}")
                }
                for (i in 0..< node.childCount){
                    val child = node.getChild(i)
                    if(child != null)deque.add(child)
                }
            }

        }
        println("Text On Screen ---> $textOnScreen")
        translateLanguage("English",textOnScreen)
    }
    private fun summarizeTextOnscreen(){
        val textOnScreen = buildString {
            if (rootInActiveWindow == null)return
            val deque: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
            deque.add(rootInActiveWindow)
            while (deque.isNotEmpty()){
                val node = deque.removeFirst()
                if(node.text != null && node.text.isNotBlank()){
                    append("${node.text}")
                }
                for (i in 0..< node.childCount){
                    val child = node.getChild(i)
                    if(child != null)deque.add(child)
                }
            }

        }
        println("Text On Screen ---> $textOnScreen")
        summarizeText(textOnScreen)
    }
    private fun summarizeText(summary:String){
        if (summary.isBlank()){
            readTextAloud("Sorry No Text Found")
            return
        }
        conversation("Summarize This Text: $summary")

    }
    private fun translateLanguage(targetLanguage:String, language:String){
        val prompt = "Translate The Below Text to $targetLanguage : $language"
        conversation(prompt)
    }
    fun showModelText(textOutput:String){
        println("About to show the model text........")
        if(windManager == null)windManager =  getSystemService(WINDOW_SERVICE) as WindowManager
        val lp = WindowManager.LayoutParams()
        lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        lp.format = PixelFormat.TRANSLUCENT

        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT

        lp.gravity = Gravity.CENTER
        if(layoutC == null){
            layoutC = FrameLayout(this)
            val inflater = LayoutInflater.from(this)
            inflater.inflate(R.layout.model_output, layoutC)
            val tv = layoutC?.findViewById<TextView>(R.id.model_output)
            tv?.text = textOutput


            val cancelBtn = layoutC?.findViewById<Button>(R.id.cancel_button)
            cancelBtn?.setOnClickListener { removeModelText() }

            println("Why this thing no show nah which kind thing be this")
            windManager?.addView(layoutC, lp)
        }else {
            val tv = layoutC?.findViewById<TextView>(R.id.model_output)
            tv?.text = textOutput
            windManager?.updateViewLayout(layoutC,lp)
        }
    }
    fun removeModelText(){
        if (layoutC != null && windManager != null){
            windManager?.removeView(layoutC)
            layoutC=null
        }
    }
    val builder = StringBuilder()
    var textTracker = 100
    var trackerHelper =0
    val progress = ProgressListener<String> { partialResult, done ->

        if(done){
            println("Model Response ---> $partialResult")
        }else{
            if(layoutB != null){
                removeProcessing()
            }
            println("Model Response else ---> $partialResult")
            if(partialResult == "n" || partialResult == " n")return@ProgressListener
            val htmlText = partialResult.replace("#","").replace("*","").replace("\\","")
            builder.append(htmlText)
            scope.launch {
                if(builder.length > textTracker){
                    readTextAloud(builder.substring(trackerHelper,builder.length))
                    trackerHelper = builder.length
                    textTracker+=100

                }
                withContext(Dispatchers.Main){
                    showModelText(builder.toString())
                }
            }
            println("Html Text ---> $htmlText")

        }
    }
    fun conversation(prompt:String){

        //interaction between user and the ai model
        //just a question asking
        println("Why not fast")
        //val modelOutput = llmInference?.generateResponse(prompt)

        llmInference?.generateResponseAsync(prompt,progress)

       // if(modelOutput != null) readTextAloud(modelOutput) else readTextAloud("Sorry Something Went Wrong")
    }
    fun processImage(prompt: String){
        val executor  = mainExecutor
        takeScreenshot(Display.DEFAULT_DISPLAY,executor,object:
            TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                showProcessing()
                scope.launch {
                    try {
                        println("On sucess")
                        val resB = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer,screenshot.colorSpace)
                        if (resB == null)return@launch
                        println("not Null")
                        val cpuBitmap: Bitmap = resB.copy(Bitmap.Config.ARGB_8888, true)
                        //showLayout(cpuBitmap)
                        imageModel(this@BetaAssistService,cpuBitmap,prompt)
                        withContext(Dispatchers.Main){
                            removeProcessing()
                        }

                    }catch (e: Exception){
                        withContext(Dispatchers.Main){
                            removeProcessing()
                        }
                        readTextAloud("Sorry Image Processing Failed.")
                    }

                }
            }

            override fun onFailure(errorCode: Int) {
                println(errorCode)
                println("Could Not Take the ScreenShot")
            }
        })
//        val intent = Intent(this, ScreenCaptureActivity::class.java).apply {
//            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required from non-Activity context
//        }
//        //windManager?.removeView(layout)
//        startActivity(intent)

    }

    private fun initializeImageModel(context:Context){

        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(10)

            .setTemperature(0.4f)
            .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
            .build()
        val taskOption = LlmInference.LlmInferenceOptions.builder()
            .setModelPath("/data/local/tmp/llm/model_version.task")
            .setMaxNumImages(1)
            .build()
        val inference = LlmInference.createFromOptions(context,taskOption)
        session = LlmInferenceSession.createFromOptions(inference,sessionOptions)
    }
    fun  imageModel(context: Context,uri: Bitmap,prompt: String = "Describe This"){
        if(session == null){
            readTextAloud("Something Went Wrong Why Processing Your Request...")
        }
        val mgImage = BitmapImageBuilder(uri).build()
//        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
//            .setTopK(10)
//
//            .setTemperature(0.4f)
//            .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
//            .build()
//        val taskOption = LlmInference.LlmInferenceOptions.builder()
//            .setModelPath("/data/local/tmp/llm/model_version.task")
//            .setMaxNumImages(1)
//            .build()
//        val inference = LlmInference.createFromOptions(context,taskOption)
//        val session = LlmInferenceSession.createFromOptions(inference,sessionOptions)
        println("About to add text chunk ............")
        session?.addQueryChunk(prompt)
        println("About to add image model.............")
        session?.addImage(mgImage)
        println("Image model Added .............")

        //val result = session.generateResponse()?:"SomeThing Went Wrong Why Processing the image"
        session?.generateResponseAsync(progress)
        scope.launch {
            delay(500)
            withContext(Dispatchers.Main){
                showProcessing()
            }
        }

    }

//Gestures

    fun rightSwipeGesture(duration: Long = 200L) {
        val metrics = this.resources.displayMetrics
        val startX = metrics.widthPixels.toFloat()/2
        val startY = metrics.heightPixels.toFloat()/2
        println("$startX:$startY")
        val endX = 0f
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, startY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(StrokeDescription(path, 0, duration))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                println("Swipe Completed.....")
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                println("Gesture Swipe cancelled")
            }
        }, null)
    }
    fun leftSwipeGesture(duration: Long = 100L) {
        val metrics = this.resources.displayMetrics
        val startX = metrics.widthPixels.toFloat()/2
        val startY = metrics.heightPixels.toFloat()/2
        val endX = metrics.widthPixels.toFloat()
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, startY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(StrokeDescription(path, 0, duration))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                println("Swipe Completed.....")
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                println("Gesture Swipe cancelled")
            }
        }, null)
    }
    fun downSwipeGesture(duration: Long = 500L) {
        val metrics = this.resources.displayMetrics
        val startX = metrics.widthPixels.toFloat()/2
        val startY = metrics.heightPixels.toFloat()/2
        val endY = 0f
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(StrokeDescription(path, 0, duration))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                println("Swipe Completed.....")
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                println("Gesture Swipe cancelled")
            }
        }, null)
    }
    fun upSwipeGesture(duration: Long = 100L) {
        val metrics = this.resources.displayMetrics
        println("Display metric height ${metrics.heightPixels}")
        val startX = metrics.widthPixels.toFloat()/2
        val startY = metrics.heightPixels.toFloat()/2
        val endY = metrics.heightPixels.toFloat()
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(StrokeDescription(path, 0, duration))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                println("Swipe Completed.....")
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                println("Gesture Swipe cancelled")
            }
        }, null)
    }
    //calbacks


    override fun onDestroy() {
        super.onDestroy()
        println("On Destroy-----> ")
        job?.cancel()
        stt?.destroy()
        tts?.shutdown()
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        speechStreamService?.stop()
        speechStreamService = null
    }
    override fun onPartialResult(hypothesis: String?) {
        Log.d("BetaAssistService", "onPartialResult")
    }

    override fun onResult(hypothesis: String?) {
        Log.d("BetaAssistService", "onResult")
        println("You Called Me ----> $hypothesis")
        val wakeWord = JSONObject(hypothesis?:"").optString("text","")
        println("Wake Word ---> $wakeWord")

        if(wakeWord.equals("gemma",true)){
            showLoading()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            speechService?.stop()
            stt?.startListening(intent)

        }
    }

    override fun onFinalResult(hypothesis: String?) {
        Log.d("BetaAssistService", "onFinalResult")
    }

    override fun onError(exception: Exception?) {
        Log.d("BetaAssistService", "onError")
    }

    override fun onTimeout() {
        Log.d("BetaAssistService", "onTimeout")
    }


}
@Serializable
data class Command(
    var function: String = ActionType.Nothing.name,
    var appName: String? = null,
    var query: String? = null,
    var prompt: String? = null
)

// Enums with lowercase names matching JSON values
enum class ActionType {
    openApp,
    extractText,
    translateText,
    summarizeTextOnscreen,
    conversation,
    describeScreen,
    Nothing,
    rightSwipeGesture,
    leftSwipeGesture,
    upSwipeGesture,
    downSwipeGesture
}
val fewshot = """
    You are an assistant that processes user requests and determines which function name to call from the BetaAssistService class. Based on the user's request, you should output a JSON object containing:
    1. A "function" field with the name of the function to call
    2. Parameter fields if the function requires parameters

    Available functions:
    - openApp(appName: String) - Opens an application
    - extractText() - Extracts and reads text from the screen
    - translateText() - Translates text on the screen
    - summarizeTextOnscreen() - Summarizes text on the screen
    - conversation(prompt: String) - Have a conversation with the AI model
    - describeScreen(prompt: String) - Describes what's visible on the screen including images
    - rightSwipeGesture() - Performs a right-to-left swipe gesture across the screen
    - leftSwipeGesture() - Performs a left-to-right swipe gesture across the screen
    - downSwipeGesture() - Performs a top-to-bottom swipe gesture on the screen
    - upSwipeGesture() - Performs a bottom-to-top swipe gesture on the screen
   

    Format your response as a valid JSON object.
    Example JSON Outputs
    {
      "function": "openApp",
      "appName": "the name of the app user want to open"
    }
    {
      "function": "describeScreen",
      "prompt": "the instruction the user gave."
    }
    
""".trimIndent()

