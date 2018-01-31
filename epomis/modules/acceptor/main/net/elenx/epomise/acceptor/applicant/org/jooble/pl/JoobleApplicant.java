package net.elenx.epomis.acceptor.applicant.org.jooble.pl;

import com.google.api.client.http.HttpResponse;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import net.elenx.epomis.acceptor.applicant.ApplicationForm;
import net.elenx.epomis.acceptor.applicant.HtmlApplicant;
import net.elenx.epomis.acceptor.applicant.resume.UserResume;
import net.elenx.epomis.service.connection6.request.DataEntry;
import net.elenx.epomis.service.connection6.response.HtmlResponse;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class JoobleApplicant implements HtmlApplicant
{
    private static final String JOBUID_SELECTOR = "input[name=JobUid]";
    private static final String SEARCHID_SELECTOR = "input[name=SearchId]";
    private static final String VIEWID_SELECTOR = "input[name=AlertViewId]";
    private static final String IMPRESSIONID_SELECTOR = "input[name=ImpressionId]";
    private static final String SESSIONCLICKID_SELECTOR = "input[name=SessionClickId]";
    private static final String JDPID_SELECTOR = "input[name=JdpId]";
    private static final String JDPACTIONID_SELECTOR = "input[name=JdpActionId]";

    @Override
    public String urlFor(ApplicationForm<HtmlResponse> applicationForm)
    {
        return "https://pl.jooble.org/ApplyForm/SendCv";
    }

    @Override
    public boolean isAppropriateFor(ApplicationForm<?> applicationForm)
    {
        return isApplicationInProgress(applicationForm) &&
            isApplicable(applicationForm);
    }

    private boolean isApplicationInProgress(ApplicationForm applicationForm)
    {
        return applicationForm.
            getStatus()
            .equals(ApplicationForm.Status.IN_PROGRESS);
    }

    private boolean isApplicable(ApplicationForm applicationForm)
    {
        return applicationForm
            .get(JoobleApplicantConsts.AVAILABLE)
            .equals(JoobleApplicantConsts.OK);
    }

    @Override
    public Map<String, String> constantHeaders()
    {
        return ImmutableMap.of
            (
                HttpHeaders.USER_AGENT, JoobleApplicantConsts.MOZILLA_USER_AGENT,
                HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded",
                HttpHeaders.X_REQUESTED_WITH, "XMLHttpRequest",
                HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br",
                HttpHeaders.ACCEPT, "*/*"
            );
    }

    @Override
    public Collection<DataEntry> userDataEntries(UserResume userResume)
    {
        return Arrays.asList
            (
                new DataEntry("CvFile", userResume.getCv().getFileName(), userResume.getCv().getInputStream()),
                new DataEntry("CvId", "0"),
                new DataEntry("CoverLetter", userResume.getCoverLetter().getFileName()),  // TO DO get if from UserResume when it will be right implemented
                new DataEntry("SaveCV", JoobleApplicantConsts.TRUE),
                new DataEntry("username", "Jan Kowalski"),
                new DataEntry("email", "jj@kolano.pl"),
                new DataEntry("agreementCheckbox", "on")
            );
    }

    @Override
    public ApplicationForm<HtmlResponse> advanceApplication(ApplicationForm<HtmlResponse> applicationForm, HtmlResponse currentResponse)
    {
        return applicationForm
            .withPreviousResponse(currentResponse)
            .withCustomData(getCustomData(currentResponse))
            .withStatus(isSuccessful(currentResponse) ? ApplicationForm.Status.SUCCESS : ApplicationForm.Status.FAILURE);
    }

    private boolean isSuccessful(HtmlResponse currentResponse)
    {
        HttpResponse httpResponse = currentResponse.getHttpResponse();
        return httpResponse.getStatusCode() == 200 &&
            httpResponse.getStatusMessage().contains(JoobleApplicantConsts.OK) &&
            getCustomData(currentResponse).size() == 7;
    }

    private Map<String, String> getCustomData(HtmlResponse currentResponse)
    {
        Document document = currentResponse.getDocument();

        Map<String, String> body = new HashMap<>();

        if (!document.select(JOBUID_SELECTOR).isEmpty())
            body.put("JobUid", document.select(JOBUID_SELECTOR).first().attr(JoobleApplicantConsts.VALUE));

        if (!document.select(SEARCHID_SELECTOR).isEmpty())
            body.put("SearchId", document.select(SEARCHID_SELECTOR).first().attr(JoobleApplicantConsts.VALUE));

        if (!document.select(VIEWID_SELECTOR).isEmpty())
            body.put("AlertViewId", document.select(VIEWID_SELECTOR).first().attr(JoobleApplicantConsts.VALUE));

        if (!document.select(IMPRESSIONID_SELECTOR).isEmpty())
            body.put("ImpressionId", document.select(IMPRESSIONID_SELECTOR).first().attr(JoobleApplicantConsts.VALUE));

        if (!document.select(SESSIONCLICKID_SELECTOR).isEmpty())
            body.put("SessionClickId", document.select(SESSIONCLICKID_SELECTOR).first().attr(JoobleApplicantConsts.VALUE));

        if (!document.select(JDPID_SELECTOR).isEmpty())
            body.put("JdpId", document.select(JDPID_SELECTOR).first().attr(JoobleApplicantConsts.VALUE));

        if (!document.select(JDPACTIONID_SELECTOR).isEmpty())
            body.put("JdpActionId", document.select(JDPACTIONID_SELECTOR).first().attr(JoobleApplicantConsts.VALUE));

        return body;
    }
}