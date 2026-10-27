package com.juanmediahubph.qrstudio.feature.generator

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.juanmediahubph.qrstudio.R
import com.juanmediahubph.qrstudio.domain.model.QrType
import com.juanmediahubph.qrstudio.domain.payload.WifiPayloadBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WifiFormComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun openNetworkHidesThePasswordField() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var fields by mutableStateOf(WifiPayloadBuilder().defaultFields())

        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                MaterialTheme {
                    GeneratorForm(
                        state = GeneratorUiState(type = QrType.WIFI, fields = fields),
                        onFieldChange = { key, value -> fields = fields + (key to value) },
                    )
                }
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.field_password)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.security_none)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.password_not_needed)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.field_password)).assertDoesNotExist()
    }
}
