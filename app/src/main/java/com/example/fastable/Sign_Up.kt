                }
        }
    }
package com.example.fastable
    private fun sendVerificationEmail(email: String) {
        val user = auth.currentUser

        if (user != null) {
            // Create ActionCodeSettings for proper email link
            val actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://fastable.page.link/verify?email=$email")
                .setHandleCodeInApp(false)
                .setAndroidPackageName(
                    "com.example.fastable",
                    true,  // installIfNotAvailable
                    null   // minimumVersion
                )
                .build()

            user.sendEmailVerification(actionCodeSettings)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        android.util.Log.d("SignUp", "Verification email sent successfully to $email")
                        Toast.makeText(
                            this,
                            "Account created! Verification email sent to $email\nPlease check your inbox and spam folder.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Redirect to login screen after short delay
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            startActivity(Intent(this, Login::class.java))
                            finish()
                        }, 2000)

                        val errorMessage = task.exception?.message ?: "Unknown error"
                        android.util.Log.e("SignUp", "Failed to send verification email: $errorMessage")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
                            "Failed to send verification email: $errorMessage\n\nTap Resend in login screen to try again.",

        auth = FirebaseAuth.getInstance()

                        // Still allow user to go to login
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            startActivity(Intent(this, Login::class.java))
                            finish()
                        }, 2000)

        val nameEt = findViewById<EditText>(R.id.name)
        val emailEt = findViewById<EditText>(R.id.email)
        val batchEt = findViewById<EditText>(R.id.batch)
        val degreeEt = findViewById<EditText>(R.id.degree)
        val sectionEt = findViewById<EditText>(R.id.section)
        val passwordEt = findViewById<EditText>(R.id.password)
import com.google.firebase.auth.ActionCodeSettings
        val confirmPasswordEt = findViewById<EditText>(R.id.confirm_password)
        val signUpBtn = findViewById<Button>(R.id.btn_signup)

        signUpBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val email = emailEt.text.toString().trim()
            val batch = batchEt.text.toString().trim()
            val degree = degreeEt.text.toString().trim()
            val section = sectionEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()
            val confirmPassword = confirmPasswordEt.text.toString().trim()

            // Debug logging
            android.util.Log.d("SignUp", "Name: '$name' (isEmpty: ${name.isEmpty()})")
            android.util.Log.d("SignUp", "Email: '$email' (isEmpty: ${email.isEmpty()})")
            android.util.Log.d("SignUp", "Batch: '$batch' (isEmpty: ${batch.isEmpty()})")
            android.util.Log.d("SignUp", "Degree: '$degree' (isEmpty: ${degree.isEmpty()})")
            android.util.Log.d("SignUp", "Section: '$section' (isEmpty: ${section.isEmpty()})")
            android.util.Log.d("SignUp", "Password: (isEmpty: ${password.isEmpty()})")
            android.util.Log.d("SignUp", "Confirm Password: (isEmpty: ${confirmPassword.isEmpty()})")

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show()
                nameEt.requestFocus()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                emailEt.requestFocus()
                return@setOnClickListener
            }
            if (batch.isEmpty()) {
                Toast.makeText(this, "Please enter your batch", Toast.LENGTH_SHORT).show()
                batchEt.requestFocus()
                return@setOnClickListener
            }
            if (degree.isEmpty()) {
                Toast.makeText(this, "Please enter your degree", Toast.LENGTH_SHORT).show()
                degreeEt.requestFocus()
                return@setOnClickListener
            }
            if (section.isEmpty()) {
                Toast.makeText(this, "Please enter your section", Toast.LENGTH_SHORT).show()
                sectionEt.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                passwordEt.requestFocus()
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show()
                confirmPasswordEt.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEt.error = "Enter valid email"
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordEt.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                confirmPasswordEt.error = "Passwords do not match"
                return@setOnClickListener
            }

            // ✅ Create user in Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                            if (verifyTask.isSuccessful) {
                                Toast.makeText(
                                    this,
                                    "Account created! Please verify your email before logging in.",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Redirect to login screen
                                startActivity(Intent(this, Login::class.java))
                                finish()
                            } else {
                                Toast.makeText(
            // Show loading toast
            Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()

                                    this,
                                    "Failed to send verification email: ${verifyTask.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                        android.util.Log.d("SignUp", "User account created successfully")
                            }

                        if (user != null) {
                            sendVerificationEmail(user.email ?: email)
                        }
                    } else {
                        val errorMessage = task.exception?.message ?: "Unknown error"
                        android.util.Log.e("SignUp", "Failed to create account: $errorMessage")
                        Toast.makeText(
                            this,
                            "Sign up failed: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    }
                }
        }
    }
}
