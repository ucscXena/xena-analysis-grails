package org.xena.analysis

import grails.plugin.json.view.JsonViewGrailsPlugin
import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import grails.validation.ValidationException
import spock.lang.Specification

import static org.springframework.http.HttpStatus.*

class TpmControllerSpec extends Specification implements ControllerUnitTest<TpmController>, DomainUnitTest<Tpm> {

  void setupSpec() {
    defineBeans(new JsonViewGrailsPlugin(applicationContext: applicationContext))
  }

  def populateValidParams(params) {
    assert params != null

    // TODO: Populate valid properties like...
    params["cohort"] = 'someValidName'
    params["data"] = 'someValidName'
    params["url"] = 'someValidName'
  }

  void "Test the index action returns the correct response"() {
    given:
    controller.tpmService = Mock(TpmService) {
      1 * list(_) >> []
      1 * count() >> 0
    }

    when: "The index action is executed"
    controller.index()

    then: "The response is correct"
    response.text == '[]'
  }


  void "Test the save action with a null instance"() {
    when:
    request.contentType = JSON_CONTENT_TYPE
    request.method = 'POST'
    controller.save()

    then:
    response.status == UNPROCESSABLE_ENTITY.value()
  }

  void "Test the save action correctly persists"() {
    given:
    controller.tpmService = Mock(TpmService) {
      1 * save(_ as Tpm)
    }

    when:
    response.reset()
    request.contentType = JSON_CONTENT_TYPE
    request.method = 'POST'
    populateValidParams(params)
    request.json = new Tpm(params)
    controller.save()

    then:
    response.status == CREATED.value()
    response.json
  }

  void "Test the save action with an invalid instance"() {
    given:
    controller.tpmService = Mock(TpmService) {
      1 * save(_ as Tpm) >> { Tpm tpm ->
        throw new ValidationException("Invalid instance", tpm.errors)
      }
    }

    when:
    request.contentType = JSON_CONTENT_TYPE
    request.method = 'POST'
    populateValidParams(params)
    request.json = new Tpm(params)
    controller.save()

    then:
    response.status == UNPROCESSABLE_ENTITY.value()
    response.json
  }

  void "Test the show action with a null id"() {
    given:
    controller.tpmService = Mock(TpmService) {
      1 * get(null) >> null
    }

    when: "The show action is executed with a null domain"
    controller.show()

    then: "A 404 error is returned"
    response.status == NOT_FOUND.value()
  }

  void "Test the show action with a valid id"() {
    given:
    controller.tpmService = Mock(TpmService) {
      1 * get(2) >> new Tpm()
    }

    when: "A domain instance is passed to the show action"
    params.id = 2
    controller.show()

    then: "A model is populated containing the domain instance"
    response.status == OK.value()
    response.json == [:]
  }

  void "Test the update action with a null instance"() {
    when:
    request.contentType = JSON_CONTENT_TYPE
    request.method = 'PUT'
    controller.update()

    then:
    response.status == UNPROCESSABLE_ENTITY.value()
  }

  void "Test the update action correctly persists"() {
    given:
    controller.tpmService = Mock(TpmService) {
      1 * save(_ as Tpm)
    }

    when:
    response.reset()
    request.contentType = JSON_CONTENT_TYPE
    request.method = 'PUT'
    populateValidParams(params)
    def instance = new Tpm(params)
    instance.id = 1
    instance.version = 0
    controller.update(instance)

    then:
    response.status == OK.value()
    response.json
  }

  void "Test the update action with an invalid instance"() {
    given:
    controller.tpmService = Mock(TpmService) {
      1 * save(_ as Tpm) >> { Tpm tpm ->
        throw new ValidationException("Invalid instance", tpm.errors)
      }
    }

    when:
    request.contentType = JSON_CONTENT_TYPE
    request.method = 'PUT'
    def instance = new Tpm(params)
    instance.id = 1
    instance.version = 0
    controller.update(instance)

    then:
    response.status == UNPROCESSABLE_ENTITY.value()
    response.json
  }

  void "Test the delete action with a null instance"() {
    when:
    request.contentType = JSON_CONTENT_TYPE
    request.method = 'DELETE'
    controller.delete()

    then:
    response.status == NOT_FOUND.value()
  }

  void "Test the delete action with an instance"() {
    given:
    controller.tpmService = Mock(TpmService) {
      1 * delete(2) >> new Tpm(id: 2)
    }

    when:
    request.contentType = JSON_CONTENT_TYPE
    request.method = 'DELETE'
    params.id = 2
    controller.delete()

    then:
    response.status == NO_CONTENT.value()
  }
}
