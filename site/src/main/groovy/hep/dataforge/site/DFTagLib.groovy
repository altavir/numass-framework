package hep.dataforge.site

import com.sysgears.grain.markup.MarkupRenderer
import com.sysgears.grain.taglib.GrainTagLib
import groovy.util.logging.Slf4j

import javax.inject.Inject

@Slf4j
class DFTagLib {

    /**
     * Grain taglib reference.
     */
    private GrainTagLib taglib

    @Inject MarkupRenderer renderer


    @Inject
    DFTagLib(GrainTagLib taglib) {
        this.taglib = taglib
    }

    /**
     * Converts a date to XML date time format: 2013-12-31T12:49:00+07:00
     *
     * @attr date the date to convert
     */
    def xmlDateTime = { Map model ->
        if (!model.date) throw new IllegalArgumentException('Tag [xmlDateTime] is missing required attribute [date]')

        def tz = String.format('%tz', model.date)

        String.format("%tFT%<tT${tz.substring(0, 3)}:${tz.substring(3)}", model.date)
    }

    def img = { String location ->
        def url = taglib.site.assets.find { it.url.endsWith(location) }.url
        return taglib.r(url)
    }

//    def note = { String text ->
//        return FixedBlock.wrapText("""
//            <hr>
//                <p>
//                    <strong> Note:</strong>
//                    ${renderer.process(text,taglib.page.markup)}
//                </p>
//            <hr>
//            """
//        )
//    }

}
