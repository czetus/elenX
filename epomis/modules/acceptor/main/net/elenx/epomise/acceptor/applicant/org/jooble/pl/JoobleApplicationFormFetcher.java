package net.elenx.epomis.acceptor.applicant.org.jooble.pl;

import com.google.api.client.http.HttpResponse;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import javafx.util.Pair;
import lombok.SneakyThrows;
import net.elenx.epomis.acceptor.applicant.ApplicationForm;
import net.elenx.epomis.acceptor.applicant.HtmlApplicant;
import net.elenx.epomis.service.connection6.response.HtmlResponse;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class JoobleApplicationFormFetcher implements HtmlApplicant
{
    private static final String[] KEYS = {"\"JobUid\"", "\"SearchId\"", "\"AlertViewId\"", "\"ImpressionId\"", "\"AwayUrl\"",
        "\"SessionClickId\"", "\"JdpId\"", "\"ResponseType\"", "\"JobTitle\"", "\"JobCity\"", "\"IsAjax\""};

    @Override
    public boolean isAppropriateFor(ApplicationForm<?> applicationForm)
    {
        return isApplicationInProgress(applicationForm) &&
            isContainsSpecificData(applicationForm) &&
            isPreviousResponseExists(applicationForm) &&
            doesBodyContainsApplyButton((HtmlResponse) applicationForm.getPreviousResponse());
    }

    private boolean isApplicationInProgress(ApplicationForm applicationForm)
    {
        return applicationForm
            .getStatus()
            .equals(ApplicationForm.Status.IN_PROGRESS);
    }

    private boolean isContainsSpecificData(ApplicationForm applicationForm)
    {
        return applicationForm.get(JoobleApplicantConsts.EPOCH) != null;
    }

    private boolean isPreviousResponseExists(ApplicationForm applicationForm)
    {
        return (applicationForm.getPreviousResponse()) != null;
    }

    private boolean doesBodyContainsApplyButton(HtmlResponse previousResponse)
    {
        return !previousResponse
            .getDocument()
            .body()
            .select("div.quick-apply_wrap")
            .select("span.quick-apply_label")
            .isEmpty();
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

    @Override
    public String urlFor(ApplicationForm<HtmlResponse> applicationForm)
    {
        String paramsFreeFromJavaScriptCode = cleanElementsFromJavaScriptCode(fetchJavaScriptCallFunction(applicationForm));
        List<String> splittedParams = new ArrayList<>(splitParamsToList(paramsFreeFromJavaScriptCode));
        splittedParams.add(JoobleApplicantConsts.TRUE);

        String encodedURL = buildEncodedURL(splittedParams);

        return buildURL(encodedURL, applicationForm);
    }

    private Elements fetchJavaScriptCallFunction(ApplicationForm<HtmlResponse> applicationForm)
    {
        return applicationForm
            .getPreviousResponse()
            .getDocument()
            .select("div.vacancy-desc-topswrap")
            .select("a[href]");
    }

    private List<String> splitParamsToList(String params)
    {
        return Arrays.asList(params.split(", "));
    }

    private String cleanElementsFromJavaScriptCode(Elements parameters)
    {
        return parameters
            .get(0)
            .attr("href")
            .replace("javascript:ApplyFormClick(", "")
            .replace("(", "")
            .replace(");", "");
    }

    @SneakyThrows(UnsupportedEncodingException.class)
    private String buildEncodedURL(List<String> urlParamsAsValue)
    {
        Map<String, String> mapParams = createURLParams(urlParamsAsValue);
        String wellFormattedURL = addPrefixASuffixToParams(mapParams);

        return URLEncoder.encode(wellFormattedURL, "UTF-8");
    }

    private Map<String, String> createURLParams(List<String> mapValues)
    {
        List<String> finalMapValues = replaceApostropheToQuote(mapValues);

        Map<String, String> paramsBuilder = new LinkedHashMap<>();

        IntStream
            .range(0, KEYS.length)
            .forEach(i -> paramsBuilder.put(KEYS[i], finalMapValues.get(i)));

        return paramsBuilder;
    }

    private List<String> replaceApostropheToQuote(List<String> mapValues)
    {
        return mapValues
            .stream()
            .map(val -> val.replace("'", "\""))
            .collect(Collectors.toList());
    }

    private String addPrefixASuffixToParams(Map<String, String> parameters)
    {
        //format example: {"key1":"value1","key2":"value2", ... }
        StringJoiner concatInRightForm = new StringJoiner(",", "{", "}");

        parameters.forEach((key,value)->joinStrings(key,value,concatInRightForm));

        return concatInRightForm.toString();
    }

    private <K,V>  void joinStrings(K key, V value, StringJoiner joiner)
    {
        joiner.add(key + ":" + value);
    }

    private String buildURL(String encodedURL, ApplicationForm applicationForm)
    {
        return new StringBuffer("https://pl.jooble.org/GetPopupAjax?name=ApplyFormPopup&model=")
            .append(encodedURL)
            .append("&_=")
            .append(applicationForm.get(JoobleApplicantConsts.EPOCH))
            .toString();
    }

    @Override
    public ApplicationForm<HtmlResponse> advanceApplication(ApplicationForm<HtmlResponse> applicationForm, HtmlResponse currentResponse)
    {
        return applicationForm
            .withStatus(isSuccessful(currentResponse) ? ApplicationForm.Status.IN_PROGRESS : ApplicationForm.Status.FAILURE)
            .withCustomData(customData(applicationForm))
            .withPreviousResponse(currentResponse);
    }

    private Map<String, String> customData(ApplicationForm<HtmlResponse> applicationForm)
    {
        return ImmutableMap.of
            (
                JoobleApplicantConsts.AVAILABLE, JoobleApplicantConsts.OK,
                JoobleApplicantConsts.SESSIONID, applicationForm.get(JoobleApplicantConsts.SESSIONID),
                JoobleApplicantConsts.EPOCH, applicationForm.get(JoobleApplicantConsts.EPOCH)
            );
    }

    @Override
    public Map<String, String> cookiesFor(ApplicationForm<HtmlResponse> applicationForm)
    {
        return ImmutableMap.of(JoobleApplicantConsts.SESSIONID, applicationForm.get(JoobleApplicantConsts.SESSIONID));
    }

    private boolean isSuccessful(HtmlResponse currentResponse)
    {
        HttpResponse httpResponse = currentResponse.getHttpResponse();

        return httpResponse.getStatusCode() == 200 &&
            httpResponse.getStatusMessage().contains(JoobleApplicantConsts.OK);
    }
}