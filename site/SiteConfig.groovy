import hep.dataforge.site.ResourceMapper
import hep.dataforge.site.DFTagLib

// Resource mapper and tag libs.
resource_mapper = new ResourceMapper(site).map
tag_libs = [DFTagLib]
datetime_format = "dd.MM.yyyy"

non_script_files = [/(?i).*\.(js|css)$/]

excludes << '.*\\.yml' << '.*\\.rb'
binary_files << '.*\\.pdf' << '.*\\.svg'

features {
    highlight = 'pygments' // 'none', 'pygments'
    markdown = 'flexmark'   // 'txtmark', 'pegdown'
//    asciidoc = 'none'
    asciidoc {
        opts = ['source-highlighter': 'coderay', 'icons': 'font']
    }
}

environments {
    dev {
        log.info 'Development environment is used'
        url = "http://localhost:${jetty_port}"
//        show_unpublished = true
    }
    prod {
        log.info 'Production environment is used'
        generate_absolute_links = true
        url = 'http://npm.mipt.ru/dataforge' // site URL, for example http://www.example.com
        show_unpublished = false
        generate_absolute_links = true
        features {
            compass = 'none'
            minify_xml = false
            minify_html = false
            minify_js = false
            minify_css = false
        }
    }
    cmd {
        features {
            compass = 'none'
            highlight = 'none'
        }
    }
}

python {
    interpreter = 'auto' // 'auto', 'python', 'jython'
    //cmd_candidates = ['python2', 'python', 'python2.7']
    //setup_tools = '2.1'
}

ruby {
    interpreter = 'jruby'   // 'auto', 'ruby', 'jruby'
    //cmd_candidates = ['ruby', 'ruby1.8.7', 'ruby1.9.3', 'user.home/.rvm/bin/ruby']
    //ruby_gems = '2.2.2'
}
