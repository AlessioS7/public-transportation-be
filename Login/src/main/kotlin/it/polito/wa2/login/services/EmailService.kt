package it.polito.wa2.login.services

import java.util.*

interface EmailService {
    fun sendCode(code: String, deadline: Date, to: String)
}