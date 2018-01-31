package net.elenx.epomis.acceptor.applicant.org.jooble.pl

import net.elenx.epomis.acceptor.applicant.Applicant
import net.elenx.epomis.acceptor.applicant.ApplicationForm
import net.elenx.epomis.acceptor.applicant.resume.UserResume
import net.elenx.epomis.acceptor.generic.ApplicantFactory
import net.elenx.epomis.acceptor.generic.ApplicantManager
import net.elenx.epomis.acceptor.generic.GenericAcceptor
import net.elenx.epomis.acceptor.model.UserFile
import net.elenx.epomis.connection.utils.FakeHttpResponseFactory
import net.elenx.epomis.entity.JobOffer
import net.elenx.epomis.router.DomainNameExtractor
import net.elenx.epomis.service.connection6.ConnectionService6
import net.elenx.epomis.service.connection6.response.HtmlResponse
import org.apache.commons.codec.net.URLCodec
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import spock.lang.Specification

import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors

class JoobleFlowTest extends MainTest {

    GenericAcceptor acceptor
    UserResume userResume
    JobOffer jobOffer
    Applicant applicantGetPreviousResponse
    Applicant applicantCheckIfCanApply
    Applicant applicantApply
    ApplicationForm<HtmlResponse> applicationForm
    ConnectionService6 connectionService
    Document document
    InputStream cvInputStream
    InputStream coverLetterStream

    def applicantFactory
    def applicantManager
    UserFile cvFile
    UserFile coverLetter
    DomainNameExtractor domainNameExtractor

    def setup() {
        acceptor = Mock()
        userResume = Mock()
        connectionService = Mock()
        domainNameExtractor = Mock()
        cvInputStream = Mock()
        cvFile = Mock()
        coverLetter = Mock()
        document = Mock()
        coverLetterStream = Mock()

        cvFile.inputStream >> cvInputStream
        cvFile.fileName >> "cv.pdf"

        coverLetter.fileName >> "aplikacja.doc"
        coverLetter.inputStream >> coverLetterStream

        userResume.cv >> cvFile
        userResume.get("firstName") >> "firstName"
        userResume.get("lastName") >> "lastName"
        userResume.get("email") >> "email"
        userResume.getCoverLetter() >> coverLetter

        jobOffer = JobOffer
            .builder()
            .href("https://pl.jooble.org/desc/9042839018962810834?ckey=java&rgn=21&pos=7&elckey=-6595122459489190497&sid=-9005058207346574551&age=3774&relb=100&brelb=100&bscr=449,71896&scr=449,71896&iid=8560172434725359579")
            .build()

        applicationForm = ApplicationForm
            .builder()
            .jobOffer(jobOffer)
            .userResume(userResume)
            .baseDomain("pl.jooble.org")
            .build()
        domainNameExtractor.extractDomain(_) >> "pl.jooble.org"
    }

    def "should apply for the offer"() {

        given: "korzystanie z metod isFound isOK ( klasa HtmlResponse) w testach powoduję błąd wykomentowanie ich bądź zastąpienie własnymi metodami sprawdzającymi naprawia problem"
        applicantGetPreviousResponse = new JoobleApplicantEntry()
        applicantCheckIfCanApply = new JoobleApplicationFormFetcher()
        applicantApply = new JoobleApplicant()

        def popupForm = JoobleFlowTest.class.getResourceAsStream("popup.html")

        applicantFactory = new ApplicantFactory(domainNameExtractor)
        applicantManager = new ApplicantManager(connectionService, [thirdApplicant, firstApplicant, secondApplicant].toSet(), applicantFactory)

        def doneResponse = createApplyResponseWithCodeAndMessage(200, "OK")

        (_..3) * connectionService.postForHtml(_) >> CompletableFuture.completedFuture(doneResponse)


        3 * doneResponse.getDocument() >> getDocumentAsFakeResponse(inputStreamA, "utf-8", withApply)
        (0..2) * doneResponse.getDocument() >> getDocumentAsFakeResponse(popupForm, "utf-8", withApply)
        (_..3) * doneResponse.getCookies() >> ['ASP.NET_SessionId': '22fsebbkb4yd0qar5hlpgaif']

        when:
        def appForm = (ApplicationForm) applicantManager.applyFor(jobOffer, userResume)
            .join()
            .stream()
            .map({ it.join() })
            .collect(Collectors.toList())
            .last()

        println appForm.status.toString()

        then:
        appForm.status == ApplicationForm.Status.SUCCESS
    }

    def createApplyResponseWithCodeAndMessage(int code, String message) {
        HtmlResponse applyStageResponse = Mock()

        def applyStageHttpResponse = FakeHttpResponseFactory.createHttpResponse(code, message)
        applyStageResponse.httpResponse >> applyStageHttpResponse

        return applyStageResponse
    }
}
