package com.example.cinet.feature.home.news

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CIViewScreen(
    selectedArticleUrl: String? = null,
    selectedArticleTitle: String? = null,
    onArticleClick: (NewsArticle) -> Unit,
    onBack: () -> Unit
) {
    val newsRepository = remember { NewsRepository() }
    var articles by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        articles = newsRepository.fetchLatestNews()
        isLoading = false
    }

    val isDining = selectedArticleTitle == "Dining"

    // System back button: go back in WebView history if available,
    // unless this is the Dining page (which the user wants to go straight home).
    BackHandler(enabled = selectedArticleUrl != null) {
        if (webView?.canGoBack() == true && !isDining) {
            webView?.goBack()
        } else {
            onBack()
        }
    }

    if (selectedArticleUrl != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = selectedArticleTitle ?: "CI View",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (webView?.canGoBack() == true && !isDining) {
                                webView?.goBack()
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        loadUrl(selectedArticleUrl)
                        webView = this
                        lastLoadedUrl = selectedArticleUrl
                    }
                },
                update = { view ->
                    if (selectedArticleUrl != null && selectedArticleUrl != lastLoadedUrl) {
                        view.loadUrl(selectedArticleUrl)
                        lastLoadedUrl = selectedArticleUrl
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("CI View") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (articles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No news found. Check your connection.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(articles) { article ->
                        NewsListItem(article = article) {
                            onArticleClick(article)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsListItem(article: NewsArticle, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = article.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = article.previewText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Read More",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}