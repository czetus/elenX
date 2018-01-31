package net.elenx.epomis.acceptor.applicant.org.jooble.pl;

import com.google.api.client.http.HttpResponse;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import net.elenx.epomis.acceptor.applicant.ApplicationForm;
import net.elenx.epomis.acceptor.applicant.HtmlApplicant;
import net.elenx.epomis.service.connection6.response.HtmlResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JoobleApplicantEntry implements HtmlApplicant
{
    @Override
    public boolean isAppropriateFor(ApplicationForm<?> applicationForm)
    {
        String offerHref = applicationForm.getJobOffer().getHref();

        return applicationForm.getCustomData().isEmpty() &&
            offerHref.contains("pl.jooble.org") &&
            canApplyOnThisSite(offerHref);
    }

    private boolean canApplyOnThisSite(String urlOfSite)
    {
        Pattern correctUrl = Pattern.compile("^(\\D+)(pl.jooble.org)(\\D{1})desc.*");

        return correctUrl
            .matcher(urlOfSite)
            .matches();
    }

    @Override
    public String urlFor(ApplicationForm<HtmlResponse> applicationForm)
    {
        return applicationForm
            .getJobOffer()
            .getHref();
    }

    @Override
    public ApplicationForm<HtmlResponse> advanceApplication(ApplicationForm<HtmlResponse> applicationForm, HtmlResponse currentResponse)
    {
        return applicationForm
            .withStatus(isSuccessful(currentResponse) ? ApplicationForm.Status.IN_PROGRESS : ApplicationForm.Status.FAILURE)
            .withCustomData(customData(currentResponse))
            .withPreviousResponse(currentResponse);
    }

    private Map<String, String> customData(HtmlResponse currentResponse)
    {
        return ImmutableMap.of
            (
                JoobleApplicantConsts.SESSIONID, currentResponse.getCookies().get(JoobleApplicantConsts.SESSIONID),
                JoobleApplicantConsts.EPOCH, String.valueOf(Instant.now().toEpochMilli())
            );
    }

    private boolean isSuccessful(HtmlResponse currentResponse)
    {
        HttpResponse httpResponse = currentResponse.getHttpResponse();

        return httpResponse.getStatusCode() == 200 &&
            httpResponse.getStatusMessage().contains(JoobleApplicantConsts.OK);
    }

    @Override
    public Map<String, String> constantHeaders()
    {
        return ImmutableMap.of(
            HttpHeaders.USER_AGENT, JoobleApplicantConsts.MOZILLA_USER_AGENT,
            HttpHeaders.ACCEPT, JoobleApplicantConsts.ACCEPT,
            "Upgrade-Insecure-Requests", "1"
        );
    }
}
