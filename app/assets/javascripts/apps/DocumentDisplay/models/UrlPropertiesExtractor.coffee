define [], ->
  class UrlPropertiesExtractor
    constructor: (@options) ->
      throw "must specify options.documentCloudUrl, an HTTPS prefix like 'https://www.documentcloud.org'" if !@options.documentCloudUrl

      documentCloudUrl = @options.documentCloudUrl
      documentCloudRegexStart = documentCloudUrl
        .replace('https:', '(?:https?:)?')
        .replace('//www.', '//(?:www.)?')
        .replace('.', '\\.')

      @extractors = [
        {
          id: 'twitter'
          name: 'Twitter tweet'
          regex: /// ^(?:https?:)?//(?:www\.)?twitter\.com/[\#!/]*([a-zA-Z0-9_]{1,15})/status(?:es)?/(\d+) ///
          capture: [ 'username', 'id' ]
          url: (o) -> "//twitter.com/#{o.username}/status/#{o.id}"
        }
        {
          id: 'facebook'
          name: 'Facebook object'
          # These aren't just posts: they can be anything
          regex: /// ^(?:https?:)?//(?:www\.)?facebook\.com/(.+) ///
          capture: [ 'path' ]
          url: (o) -> "//www.facebook.com/#{o.path}"
        }
        {
          id: 'documentCloud'
          name: 'DocumentCloud document'
          regex: /// ^#{documentCloudRegexStart}/documents/([-a-zA-Z0-9]+)(\#p[0-9]+)? ///
          capture: [ 'id', 'page' ]
          url: (o) -> "#{documentCloudUrl}/documents/#{o.id}.html"
        }
        {
          id: 'pdf'
          name: 'PDF'
          regex: /// ^/documents/(\d+).pdf$ ///
          capture: [ 'id' ]
          url: (o) -> "/documents/#{o.id}.pdf"        # unused, comes from Document.viewUrl for pdf.js
          }
        {
          id: 'pdf'
          name: 'PDF file served from server local storage'
          regex: /// ^local://(.+) ///
          capture: [ 'fileName' ]
          url: (o) -> "/localfiles/#{o.fileName}"     # unused, comes from Document.viewUrl for pdf.js
        }
        {
          id: 'https'
          name: 'Secure web page'
          regex: /// ^(https://.*) ///
          capture: [ 'rawUrl' ]
          url: (o) -> o.rawUrl
        }
        {
          id: 'http'
          name: 'Insecure web page'
          regex: /// ^(http://.*) ///
          capture: [ 'rawUrl' ]
          url: (o) -> o.rawUrl
        }
        {
          id: 'unknown'
          name: 'Unknown'
          regex: /// ^(.+) ///
          capture: [ 'rawUrl' ]
          url: (o) -> o.rawUrl
        }
        {
          id: 'none'
          name: 'None'
          regex: /// ^$ ///
          capture: []
          url: (o) -> ''
        }
      ]

    # Returns some URL-derived properties, given a URL
    #
    # For instance:
    #
    #   extractor = new UrlPropertiesExtractor(documentCloudUrl: "https://www.documentcloud.org")
    #   extractor.urlToProperties("https://twitter.com/adamhooper/status/1231241324")
    #   ==> {
    #     type: 'twitter',
    #     typeName: 'Twitter tweet',
    #     username: 'adamhooper',
    #     id: '1231241324',
    #     url: <better URL>
    #   }
    #
    # The returned URL is "better": it replaces http:// or https:// with plain //.
    urlToProperties: (url) ->
      ret = undefined
      url ?= ''

      for extractor in @extractors
        if m = url.match(extractor.regex)
          ret = { type: extractor.id, typeName: extractor.name }
          for name, i in extractor.capture
            ret[name] = m[i + 1]
          ret.url = extractor.url(ret)
          break

      ret
