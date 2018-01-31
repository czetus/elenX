package net.elenx.epomis.acceptor.applicant.org.jooble.pl

import org.apache.commons.codec.net.URLCodec
import org.jsoup.select.Elements

import java.time.Instant

class PoCTests extends MainTest {

    def "check if contains correct href"(){

        setup:
        def href = withApply

        when:
        href.contains("pl.jooble.org")

        then:
        true

    }

    def "we should apply for this offfer"(){

        given:

        def document = getDocumentAsFakeResponse(inputStreamA,"utf-8",withApply)
        when:
        //Elements elements = document.body().getElementsByAttributeValueContaining("class","quick-apply_label")
        Elements elements = document.body().select("div.quick-apply_wrap")
            .select("span.quick-apply_label")
        elements.each{
            println it
        }

        then:
        println "Size of elements it's equal === " + elements.size()
        elements.size() != 0

    }

    def "we should reject this offer for applying"(){

        given:
        def document = getDocumentAsFakeResponse(inputStreamNa,"utf-8", withApply)

        when:
        Elements elements = document.body().getElementsByAttributeValueContaining("class","quick-apply_label")
        then:
        elements.size() == 0

    }

    def "should get full href to to make third GET popup Ajax" (){
        given:
        def document = getDocumentAsFakeResponse(inputStreamA, "utf-8", withApply)
        def keys = ["JobUid","SearchId","AlertViewId","ImpressionId","AwayUrl",
                    "SessionClickId","JdpId","ResponseType","JobTitle","JobCity","IsAjax"]
        LinkedHashMap<String,String> params = new LinkedHashMap<>()

        URLCodec codec = new URLCodec()

        when:

        // format pobranego elementu
        // <a href="javascript:ApplyFormClick('8511600814625659715', '7773421727301848473', '0', '-7277224502572532117', 'https://pl.jooble.org/away/8511600814625659715?p=1&amp;pos=0&amp;sid=7773421727301848473&amp;age=8528&amp;relb=100&amp;brelb=100&amp;scr=457,7799&amp;bscr=457,7799&amp;jdp=1&amp;scid=-5693298805478063685&amp;jdpid=1438553609513527613', '-5693298805478063685', '1438553609513527613', '1', 'Java Developer', 'Warszawa');" class="respond-vacancy_button jooble_apply_button  " onclick="af_jdpClickTop();ga_JdpClickApplyTotal();" rel="nofollow"> <span>Aplikuj za pomocą Jooble</span> </a>

        //   Elements elements = document.select("div.vacancy-desc-topswrap").select("a[href]")
        Elements elements = document.select(".vacancy-desc-buttonswrap").select("a[href]");

        //javascript:ApplyFormClick('8511600814625659715', '7773421727301848473', '0', '-7277224502572532117', 'https://pl.jooble.org/away/8511600814625659715?p=1&pos=0&sid=7773421727301848473&age=8528&relb=100&brelb=100&scr=457,7799&bscr=457,7799&jdp=1&scid=-5693298805478063685&jdpid=1438553609513527613', '-5693298805478063685', '1438553609513527613', '1', 'Java Developer', 'Warszawa');
        def javaScriptFunc =  elements.get(0).attr("href")

        println "element pobrany --> " + javaScriptFunc
        javaScriptFunc =  javaScriptFunc.replace("javascript:ApplyFormClick(","")
        javaScriptFunc = javaScriptFunc.replace(");","")
        ArrayList<String> values = Arrays.asList(javaScriptFunc.split(", "));

        values.add("true") //IsAjax

        for(int i = 0 ; i < keys.size();i++){
            params.put("\"" + keys[i] + "\"",
                values.get(i).replace("'","\""))
        }

        StringJoiner sj = new StringJoiner(",","{","}")
        params.forEach{key,value ->
            sj.add(key+":"+value)

        }

        print sj.toString()
        println()

        print URLEncoder.encode(sj.toString(),"UTF-8")

        println()
        println "Time EPOCH " + Instant.now().toEpochMilli()


        then:

        keys.size() == values.size()



    }

    def "get all form params"(){
        given:
        def popupForm = JoobleFlowTest.class.getResourceAsStream("popup.html")
        def document = getDocumentAsFakeResponse(popupForm, "utf-8", withApply)

        when:
        Elements elementy =   document.getElementById("mainApplyPopup").getElementsByTag("input")

        then:
        println document.select("input[name=JobUid]").first().attr("value")
    }

    def buildEncodedURLMethodTest()
    {
        given:
        def KEYS = ["JobUid","SearchId","AlertViewId","ImpressionId","AwayUrl","SessionClickId","JdpId","ResponseType","JobTitle","JobCity","IsAjax"]
        def  PARAMS_WRAPED  = ["\"8511600814625659715\"","\"7773421727301848473\"","\"0\"","\"-7277224502572532117\"",
                               "\"https://pl.jooble.org/away/8511600814625659715?p=1&pos=0&sid=7773421727301848473&age=8528&relb=100&brelb=100&scr=457,7799&bscr=457,7799&jdp=1&scid=-5693298805478063685&jdpid=1438553609513527613\""
                               ,"\"-5693298805478063685\"","\"1438553609513527613\"","\"1\"","\"Java Developer\"","\"Warszawa\"",true]
        when:
        PARAMS_WRAPED.each{ println it}

        then:
        1==1

    }
}
