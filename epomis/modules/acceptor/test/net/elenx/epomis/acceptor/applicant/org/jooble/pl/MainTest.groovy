package net.elenx.epomis.acceptor.applicant.org.jooble.pl

import org.jsoup.Jsoup
import spock.lang.Specification

abstract class MainTest extends Specification{
    protected InputStream inputStreamNa = JoobleFlowTest.class.getResourceAsStream("JoobleNoApplication.html")
    protected withApply = "https://pl.jooble.org/desc/9042839018962810834?ckey=java&rgn=21&pos=7&elckey=-6595122459489190497&sid=-9005058207346574551&age=3774&relb=100&brelb=100&bscr=449,71896&scr=449,71896&iid=8560172434725359579"
    protected InputStream inputStreamA = JoobleFlowTest.class.getResourceAsStream("joobleApplication.html")

    org.jsoup.nodes.Document getDocumentAsFakeResponse (InputStream source, String charset, String URL){
        def document = Jsoup.parse(source,charset,URL)
    }
}
