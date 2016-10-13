define [ 'jquery', 'apps/DocumentCloudImportForm/app', 'apps/ImportOptions/app' ], ($, ImportApp, OptionsApp) ->
  $ ->
    el = document.getElementById('document-cloud-import-job')
    query = el.getAttribute('data-query')
    submitUrl = el.getAttribute('data-submit-url')

    optionsApp = new OptionsApp
      onlyOptions: [ 'lang', 'split_documents' ]
      supportedLanguages: window.supportedLanguages
      defaultLanguageCode: window.defaultLanguageCode

    importApp = new ImportApp query, submitUrl,
      extraOptionsEl: optionsApp.fieldsetEl

    el.appendChild(importApp.el)
