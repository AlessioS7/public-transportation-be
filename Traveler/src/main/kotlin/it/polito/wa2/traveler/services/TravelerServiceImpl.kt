package it.polito.wa2.traveler.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.traveler.*
import it.polito.wa2.traveler.dtos.TicketPurchasedDTO
import it.polito.wa2.traveler.dtos.TransitDTO
import it.polito.wa2.traveler.dtos.toDTO
import it.polito.wa2.traveler.entities.TicketPurchased
import it.polito.wa2.traveler.entities.Transit
import it.polito.wa2.traveler.entities.UserDetails
import it.polito.wa2.traveler.exceptions.InvalidJwtException
import it.polito.wa2.traveler.exceptions.TicketNotFoundException
import it.polito.wa2.traveler.exceptions.UserNotFoundException
import it.polito.wa2.traveler.repositories.TicketPurchasedRepository
import it.polito.wa2.traveler.repositories.TransitRepository
import it.polito.wa2.traveler.repositories.UserDetailsRepository
import it.polito.wa2.traveler.security.JwtUtils
import it.polito.wa2.traveler.util.JwtUtilsTicket
import it.polito.wa2.traveler.util.QRCodeGenerator.generateQRCodeImage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.*
import javax.transaction.Transactional


@Service
@Transactional
class TravelerServiceImpl : TravelerService {
    @Autowired
    lateinit var jwtUtils: JwtUtils

    @Autowired
    lateinit var jwtUtilsTicket: JwtUtilsTicket

    @Value("\${login.token.prefix}")
    lateinit var tokenPrefix: String

    @Value("\${login.jwt.expiration}")
    lateinit var expiration: String

    @Value("\${traveler.jwt.key}")
    lateinit var ticketKey: String

    @Autowired
    lateinit var userDetailsRepository: UserDetailsRepository

    @Autowired
    lateinit var ticketRepository: TicketPurchasedRepository

    @Autowired
    lateinit var transitRepository: TransitRepository

    override fun getMyProfile(): UserDetailsResponse {
        val name = SecurityContextHolder.getContext().authentication.principal.toString()
        val userDetails = userDetailsRepository.findByName(name)

        if (!userDetails.isPresent)
            throw UserNotFoundException()

        return UserDetailsResponse(
            name = userDetails.get().name,
            address = userDetails.get().address,
            dateOfBirth = userDetails.get().dateOfBirth,
            telephoneNumber = userDetails.get().telephoneNumber
        )
    }

    override fun updateMyProfile(authToken: String, userDetails: UserDetailsRequest) {
        val token = removeBearer(authToken)

        if (!jwtUtils.validateJwt(token))
            throw InvalidJwtException()

        val userId = jwtUtils.getJwtClaims(token).body.id.toLong()

        userDetailsRepository.save(
            UserDetails().apply {
                id = userId
                name = userDetails.name
                address = userDetails.address
                dateOfBirth = userDetails.dateOfBirth
                telephoneNumber = userDetails.telephoneNumber
            }
        )
    }

    override fun getTickets(): List<TicketResponse?> {
        val name = SecurityContextHolder.getContext().authentication.principal.toString()
        val user = userDetailsRepository.findByName(name)

        if (user.isEmpty)
            throw UserNotFoundException()

        val tickets = mutableListOf<TicketResponse?>()

        for (ticket in ticketRepository.findAllByUser(user.get())) {
            getTicketDetailsFromJwt(ticket.jwt)?.let {
                tickets.add(it)
            }
        }

        return tickets
    }

    override fun getTicket(ticketID: String): BufferedImage? {
        val name = SecurityContextHolder.getContext().authentication.principal.toString()
        val user = userDetailsRepository.findByName(name)

        if (user.isEmpty)
            throw UserNotFoundException()

        val ticket = ticketRepository.findByUuidAndUser(UUID.fromString(ticketID), user.get())
            ?: throw TicketNotFoundException()

        return generateQRCodeImage(ticket.jwt)


    }

    override fun buyTickets(ticketRequest: TicketRequest): List<TicketResponse?> {
        val name = SecurityContextHolder.getContext().authentication.principal.toString()
        val user = userDetailsRepository.findByName(name)

        if (user.isEmpty)
            throw UserNotFoundException()

        val tickets = mutableListOf<TicketResponse?>()

        for (i in 0 until ticketRequest.quantity.toInt()) {
            val uuid = UUID.randomUUID()

            val issuedAt = Date()
            var exp: Date
            var validfrom: Date? = null

            if (ticketRequest.validdays.isEmpty()) { //ticket with fixed duration
                validfrom = Date()
                exp = Date(System.currentTimeMillis() + (ticketRequest.duration.toLong() * 1000L))
            } else { //duration is the one inside the validdays
                val tmpDays = ticketRequest.validdays.map { if (it == 1) 7 else it - 1 }
                val maxDay = if (tmpDays.maxOf { it } == 7) 1 else tmpDays.maxOf { it + 1 }
                val minDay = if (tmpDays.minOf { it } == 7) 1 else tmpDays.minOf { it + 1 }

                val minDate = Calendar.getInstance()
                minDate.set(Calendar.SECOND, 0)
                minDate.set(Calendar.MINUTE, 0)
                minDate.set(Calendar.HOUR_OF_DAY, 0)

                if (ticketRequest.validdays.contains(minDate.get(Calendar.DAY_OF_WEEK))) {
                    validfrom = Date(minDate.timeInMillis)
                } else {
                    while (minDate.get(Calendar.DAY_OF_WEEK) != minDay) {
                        minDate.add(Calendar.DATE, 1)
                    }
                    validfrom = Date(minDate.timeInMillis)
                }

                while (minDate.get(Calendar.DAY_OF_WEEK) != maxDay) {
                    minDate.add(Calendar.DATE, 1)
                }
                minDate.set(Calendar.SECOND, 59)
                minDate.set(Calendar.MINUTE, 59)
                minDate.set(Calendar.HOUR_OF_DAY, 23)
                exp = Date(minDate.timeInMillis)
            }


            val jwt = Jwts.builder()
                .setSubject(uuid.toString())
                .setIssuedAt(issuedAt)
                .setExpiration(exp)
                .setNotBefore(validfrom)
                .claim("type", ticketRequest.type)
                .addClaims(mapOf("zid" to ticketRequest.zones))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(ticketKey)))
                .compact()

            tickets.add(getTicketDetailsFromJwt(jwt))

            ticketRepository.save(
                TicketPurchased().apply {
                    this.uuid = uuid
                    this.jwt = jwt
                    this.user = user.get()
                }
            )

        }

        return tickets
    }

    override fun getTravelerNames(): List<String> {
        return userDetailsRepository.getAllNames()
    }

    override fun getTravelerDetails(userId: Long): UserDetailsResponse {
        val user = userDetailsRepository.findFirstById(userId)

        if (user.isEmpty)
            throw UserNotFoundException()

        return UserDetailsResponse(
            name = user.get().name,
            address = user.get().address,
            dateOfBirth = user.get().dateOfBirth,
            telephoneNumber = user.get().telephoneNumber
        )
    }

    override fun registerTransit(transitRequest: TransitRequest) {
        val ticketId = UUID.fromString(transitRequest.ticketID)
        val ticket = ticketRepository.findFirstByUuid(ticketId)
            ?: throw TicketNotFoundException()

        val type = jwtUtilsTicket.getJwtTicketType(ticket.jwt)

        val user = ticket.user ?: throw UserNotFoundException()

        transitRepository.save(Transit().apply {
            uuid = UUID.randomUUID()
            userId = user.id
            turnstile = SecurityContextHolder.getContext().authentication.principal.toString()
            ticketType = type
        })

    }

    override fun getTravelerTickets(userId: Long): List<TicketResponse> {
        val user = userDetailsRepository.findFirstById(userId)

        if (user.isEmpty)
            throw UserNotFoundException()

        val ticketsPurchased = mutableListOf<TicketResponse>()

        for (ticket in ticketRepository.findAllByUser(user.get())) {
            if (getTicketDetailsFromJwt(ticket.jwt) != null)
                ticketsPurchased.add(getTicketDetailsFromJwt(ticket.jwt)!!)
        }

        return ticketsPurchased
    }

    override fun getPurchasesReport(from: String?, to: String?, userId: Long?): List<TicketPurchasedDTO> {
        var fromDate: Date? = null
        var toDate: Date? = null
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm")

        formatter.timeZone = TimeZone.getTimeZone("UTC")

        if (from != null)
            fromDate = formatter.parse(from)

        if (to != null)
            toDate = formatter.parse(to)

        var user: UserDetails? = null
        if (userId != null) {
            if (userDetailsRepository.existsById(userId))
                user = userDetailsRepository.findFirstById(userId).get()
            else throw UserNotFoundException()
        }

        if (fromDate != null && toDate != null && user != null)
            return ticketRepository
                .findAllByDateBetweenAndUser(fromDate, toDate, user)
                .map { it.toDTO() }

        if(fromDate != null && toDate != null)
            return ticketRepository
                .findAllByDateBetween(fromDate, toDate)
                .map { it.toDTO() }

        if(fromDate != null && user != null)
            return ticketRepository
                .findAllByDateAfterAndUser(fromDate, user)
                .map { it.toDTO() }

        if(fromDate != null)
            return ticketRepository
                .findAllByDateAfter(fromDate)
                .map { it.toDTO() }

        if(toDate != null && user != null)
            return ticketRepository
                .findAllByDateBeforeAndUser(toDate, user)
                .map { it.toDTO() }

        if(toDate != null)
            return ticketRepository
                .findAllByDateBefore(toDate)
                .map { it.toDTO() }

        if(user != null)
            return ticketRepository
                .findAllByUser(user)
                .map { it.toDTO() }

        return ticketRepository.findAll().map { it.toDTO() }
    }

    override fun getTransitsReport(from: String?, to: String?, userId: Long?): List<TransitDTO> {
        var fromDate: Date? = null
        var toDate: Date? = null
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm")

        formatter.timeZone = TimeZone.getTimeZone("UTC")

        if (from != null)
            fromDate = formatter.parse(from)

        if (to != null)
            toDate = formatter.parse(to)

        if (userId != null && !userDetailsRepository.existsById(userId)) {
                throw UserNotFoundException()
        }

        if (fromDate != null && toDate != null && userId != null)
            return transitRepository
                .findAllByDateBetweenAndUserId(fromDate, toDate, userId)
                .map { it.toDTO() }

        if(fromDate != null && toDate != null)
            return transitRepository
                .findAllByDateBetween(fromDate, toDate)
                .map { it.toDTO() }

        if(fromDate != null && userId != null)
            return transitRepository
                .findAllByDateAfterAndUserId(fromDate, userId)
                .map { it.toDTO() }

        if(fromDate != null)
            return transitRepository
                .findAllByDateAfter(fromDate)
                .map { it.toDTO() }

        if(toDate != null && userId != null)
            return transitRepository
                .findAllByDateBeforeAndUserId(toDate, userId)
                .map { it.toDTO() }

        if(toDate != null)
            return transitRepository
                .findAllByDateBefore(toDate)
                .map { it.toDTO() }

        if(userId != null)
            return transitRepository
                .findAllByUserId(userId)
                .map { it.toDTO() }

        return transitRepository.findAll().map { it.toDTO() }
    }

    private fun removeBearer(authToken: String): String {
        return authToken.replace(tokenPrefix, "")
    }

    private fun getTicketDetailsFromJwt(token: String): TicketResponse? {
        return try {
            val claims = Jwts
                .parserBuilder()
                .setSigningKey(ticketKey)
                .build()
                .parseClaimsJws(token)
            println("parsing done")

            TicketResponse(
                sub = claims.body.subject,
                iat = claims.body.issuedAt.toString(),
                exp = claims.body.expiration.toString(),
                nbf = claims.body.notBefore.toString(),
                type = claims.body["type"].toString(),
                zid = claims.body["zid"].toString(),
                jws = token
            )
        } catch (e: Exception) {
            null
        }

    }
}


