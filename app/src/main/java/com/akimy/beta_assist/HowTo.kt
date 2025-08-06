package com.akimy.beta_assist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun TutorialContent(modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header section
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "βA",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "BetaAssist",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Your AI-powered accessibility assistant",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Introduction section
        item {
            SectionCard(
                title = "What is BetaAssist?",
                content = "BetaAssist is an accessibility service powered by Gemma 3n, " +
                        "designed to make your device more powerful through voice commands. " +
                        "It can help you navigate your device, read text, translate content, " +
                        "and much more - all through simple voice interactions."
            )
        }

        // How to use section
        item {
            SectionCard(
                title = "How to Use",
                content = ""
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StepItem(
                        number = 1,
                        title = "Activate BetaAssist",
                        description = "Enable BetaAssist in your device's Accessibility Settings"
                    )

                    StepItem(
                        number = 2,
                        title = "Say the wake word",
                        description = "Simply say \"Gemma\" to activate the assistant"
                    )

                    StepItem(
                        number = 3,
                        title = "Speak your command",
                        description = "After the activation sound, speak your request clearly"
                    )

                    StepItem(
                        number = 4,
                        title = "Listen for response",
                        description = "BetaAssist will process your request and respond by voice"
                    )
                }
            }
        }

        // Features section
        item {
            SectionCard(
                title = "Key Features",
                content = ""
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureItem(
                        title = "App Navigation",
                        description = "Open any installed app with a voice command",
                        example = "\"Open Calendar\""
                    )

                    FeatureItem(
                        title = "Text Extraction",
                        description = "Read text from your screen aloud",
                        example = "\"Read this page\""
                    )

                    FeatureItem(
                        title = "Translation",
                        description = "Translate on-screen text to English",
                        example = "\"Translate this\""
                    )

                    FeatureItem(
                        title = "Summarization",
                        description = "Get a concise summary of screen content",
                        example = "\"Summarize this page\""
                    )

                    FeatureItem(
                        title = "Visual Description",
                        description = "Describe what's visible on screen",
                        example = "\"What am I looking at?\""
                    )

                    FeatureItem(
                        title = "Conversation",
                        description = "Ask questions or chat with the AI assistant",
                        example = "\"What's the weather like?\""
                    )

                    FeatureItem(
                        title = "Shopping Assistant",
                        description = "Help with Jumia product searches",
                        example = "\"Find shoes on Jumia\""
                    )
                }
            }
        }

        // Tips section
        item {
            SectionCard(
                title = "Tips for Best Results",
                content = ""
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipItem("Speak clearly and at a moderate pace")
                    TipItem("Use the wake word \"Gemma\" before each new command")
                    TipItem("Wait for the activation sound before speaking")
                    TipItem("Be specific in your requests")
                    TipItem("For better responses, ensure good microphone access")
                    TipItem("If BetaAssist doesn't respond, try repeating the wake word")
                }
            }
        }

        // Footer
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "Powered by Gemma 3n",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
    contentComposable: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (content.isNotEmpty()) {
                Text(
                    text = content,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            contentComposable?.invoke()
        }
    }
}

@Composable
fun StepItem(number: Int, title: String, description: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FeatureItem(title: String, description: String, example: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Example: ")
                    }
                    append(example)
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun TipItem(tip: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle, // Make sure this resource exists
            contentDescription = "Tip",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = tip,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}