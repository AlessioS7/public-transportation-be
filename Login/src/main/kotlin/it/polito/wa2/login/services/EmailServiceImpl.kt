package it.polito.wa2.login.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.util.*


@Service
class EmailServiceImpl : EmailService {
    @Autowired
    lateinit var emailSender: JavaMailSender

    override fun sendCode(code: String, deadline: Date, to: String) {
        val message = SimpleMailMessage()
        message.setFrom("webapp2g8@gmail.com")
        message.setTo(to)
        message.setSubject("Email verification code")
        message.setText("This is the code to verify your email address:\n$code\nThe code must be inserted before $deadline.")
        emailSender.send(message)
    }
}