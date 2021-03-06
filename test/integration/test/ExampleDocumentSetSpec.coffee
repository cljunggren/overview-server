asUser = require('../support/asUser')
shouldBehaveLikeATree = require('../support/behave/likeATree')
testMethods = require('../support/testMethods')
wd = require('wd')


Url =
  index: '/documentsets'
  show: /\/documentsets\/(\d+)/
  csvUpload: '/imports/csv'
  publicDocumentSets: '/public-document-sets'

userToTrXPath = (email) -> "//tr[contains(td[@class='email'], '#{email}')]"

describe 'ExampleDocumentSets', ->
  testMethods.usingPromiseChainMethods
    waitForUserLoaded: (email) ->
      @
        .waitForElementByXPath(userToTrXPath(email))

    openCsvUploadPage: ->
      @
        .get(Url.csvUpload)
        .waitForJqueryReady()

    chooseFile: (path) ->
      fullPath = "#{__dirname}/../files/#{path}"
      @
        .elementByCss('input[type=file]').sendKeys(fullPath)

    cloneExample: ->
      @
        .waitForJqueryReady()
        .waitForElementBy(tag: 'button', contains: 'Clone').click()
        .waitForUrl(Url.show, 10000)

    toggleExampleDocumentSet: ->
      checkbox = { tag: 'label', contains: 'Set as example document set', visible: true }

      @
        .waitForJqueryReady()
        .elementByCss('nav .dropdown-toggle a').click()
        .elementByCss('.dropdown-menu .show-sharing-settings').click()
        .frame('share-document-set')
        .waitForElementBy(checkbox)
        .listenForJqueryAjaxComplete()
        .elementBy(checkbox).click()
        .waitForJqueryAjaxComplete()
        .frame(null)
        .elementByCss('#sharing-options-modal a[data-dismiss=modal]').click()

    waitForRequirements: ->
      @
        .waitForFunctionToReturnTrueInBrowser(-> $('.requirements li.ok').length == 4 || $('.requirements li.bad').length > 0)

    doUpload: ->
      @
        .elementBy(tag: 'button', contains: 'Upload').click()
        .waitForUrl(Url.show, 10000)

    chooseAndDoUpload: (path) ->
      @
        .chooseFile(path)
        .waitForRequirements()
        .doUpload()

    deleteTopUpload: ->
      @
        .get(Url.index)
        .elementByCss('.actions .dropdown-toggle').click()
        .acceptingNextAlert()
        .elementByCss('.dropdown-menu .delete-document-set').click()

    waitForJobsToComplete: ->
      @.waitForFunctionToReturnTrueInBrowser((-> $?.isReady && $('progress').length == 0), 15000)

  asUser.usingTemporaryUser(title: 'ExampleDocumentSets', adminBrowser: true)

  describe 'after being set as an example', ->
    before ->
      @adminBrowser
        .openCsvUploadPage()
        .chooseAndDoUpload('CsvUpload/basic.csv')
        .waitForJobsToComplete()
        .toggleExampleDocumentSet()
        .then =>
          @userBrowser
            .get(Url.publicDocumentSets)
            .cloneExample()

    after ->
      Q.all([
        @userBrowser.deleteTopUpload()
        @adminBrowser.deleteTopUpload()
      ])

    it 'should be cloneable',  ->
      @userBrowser
        .get(Url.index)
        .waitForElementBy(tag: 'h3', contains: 'basic.csv').should.eventually.exist

    it 'should be removed from the example list when unset as an example', ->
      @adminBrowser.toggleExampleDocumentSet()
        .then =>
          @userBrowser
            .get(Url.publicDocumentSets)
            .waitForElementBy(tag: 'p', contains: 'There are currently no example document sets.').should.eventually.exist
        .then =>
          @adminBrowser.toggleExampleDocumentSet() # return to original state

    describe 'the cloned example', ->
      before ->
        @userBrowser
          .get(Url.index)
          .waitForElementBy(tag: 'a', contains: 'basic.csv').click()
          .waitForElementBy(tag: 'canvas')

      shouldBehaveLikeATree
        documents: [
          { type: 'text', title: 'Fourth', contains: 'This is the fourth document.' }
        ]
        searches: [
          { query: 'document', nResults: 4 }
        ]

    it 'should keep clone after original is deleted', ->
      @adminBrowser
        .openCsvUploadPage()
        .chooseAndDoUpload('CsvUpload/basic.csv')
        .waitForJobsToComplete()
        .toggleExampleDocumentSet()
        .then =>
          @userBrowser
            .get(Url.publicDocumentSets)
            .cloneExample()
        .then =>
          @adminBrowser
            .deleteTopUpload()
        .then =>
          @userBrowser
            .get(Url.index)
            .waitForElementBy(tag: 'a', contains: 'basic.csv').should.eventually.exist
            .deleteTopUpload()
