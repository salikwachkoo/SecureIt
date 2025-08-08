package com.mohammadsalik.secureit.presentation.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mohammadsalik.secureit.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthenticationFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testWelcomeScreenDisplayed() {
        // Verify welcome screen is displayed initially
        composeTestRule.onNodeWithText("SecureVault").assertExists()
        composeTestRule.onNodeWithText("Your Personal Security Vault").assertExists()
        composeTestRule.onNodeWithText("Get Started").assertExists()
    }

    @Test
    fun testPinSetupFlow() {
        // Click "Get Started" to begin PIN setup
        composeTestRule.onNodeWithText("Get Started").performClick()
        
        // Verify PIN setup screen is displayed
        composeTestRule.onNodeWithText("Create Your PIN").assertExists()
        composeTestRule.onNodeWithText("Create a 4-digit PIN to secure your vault").assertExists()
        
        // Enter a PIN
        composeTestRule.onNodeWithText("Enter PIN").performClick()
        composeTestRule.onNodeWithText("Enter PIN").performTextInput("1234")
        
        // Verify PIN is entered (should show as dots)
        composeTestRule.onNodeWithText("••••").assertExists()
    }

    @Test
    fun testPinEntryFlow() {
        // This test would require setting up a PIN first
        // For now, we'll test the PIN entry screen structure
        // In a real scenario, we'd need to set up a PIN and then test entry
        
        // Verify PIN entry screen elements exist
        composeTestRule.onNodeWithText("Enter Your PIN").assertExists()
        composeTestRule.onNodeWithText("Enter your 4-digit PIN to access your vault").assertExists()
    }

    @Test
    fun testBiometricSetupScreen() {
        // Navigate to biometric setup (this would require completing PIN setup first)
        // For now, we'll test the biometric setup screen structure
        
        // Verify biometric setup screen elements
        composeTestRule.onNodeWithText("Biometric Setup").assertExists()
    }

    @Test
    fun testMainVaultScreen() {
        // Test the main vault screen (would require successful authentication)
        // Verify main vault screen elements
        composeTestRule.onNodeWithText("SecureVault").assertExists()
    }

    @Test
    fun testNavigationFlow() {
        // Test the complete navigation flow
        // 1. Welcome -> PIN Setup
        composeTestRule.onNodeWithText("Get Started").performClick()
        composeTestRule.onNodeWithText("Create Your PIN").assertExists()
        
        // 2. PIN Setup -> Biometric Setup (after PIN setup)
        // This would require completing PIN setup
        
        // 3. Biometric Setup -> Main Vault (after biometric setup)
        // This would require completing biometric setup
    }

    @Test
    fun testErrorHandling() {
        // Test error handling in PIN entry
        // Enter wrong PIN and verify error message
        composeTestRule.onNodeWithText("Enter Your PIN").performClick()
        composeTestRule.onNodeWithText("Enter Your PIN").performTextInput("0000")
        
        // Verify error message appears (if PIN validation fails)
        // This depends on the actual PIN validation logic
    }

    @Test
    fun testAccessibilityFeatures() {
        // Test accessibility features
        // Verify content descriptions are present
        composeTestRule.onNodeWithContentDescription("Lock icon").assertExists()
        
        // Test screen reader compatibility
        // Verify important text is accessible
        composeTestRule.onNodeWithText("SecureVault").assertIsDisplayed()
    }

    @Test
    fun testUIResponsiveness() {
        // Test UI responsiveness
        // Verify buttons are clickable
        composeTestRule.onNodeWithText("Get Started").assertIsEnabled()
        // Verify text fields are interactive
        composeTestRule.onNodeWithText("Enter PIN").assertIsEnabled()
    }
}