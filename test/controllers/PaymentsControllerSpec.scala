/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.ConfigDecorator
import connectors._
import controllers.auth.requests.UserRequest
import controllers.auth.{AuthJourney, WithBreadcrumbAction}
import error.ErrorRenderer
import models.CreatePayment
import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.mvc.{MessagesControllerComponents, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.CurrentTaxYear
import util.UserRequestFixture.buildUserRequest
import util.{ActionBuilderFixture, BaseSpec}
import views.html.{ErrorView, NotFoundView}

import scala.concurrent.{ExecutionContext, Future}

class PaymentsControllerSpec extends BaseSpec with CurrentTaxYear with MockitoSugar {

  override def now: () => DateTime = DateTime.now

  lazy val fakeRequest = FakeRequest("", "")

  val mockPayConnector = mock[PayApiConnector]
  val mockAuthJourney = mock[AuthJourney]

  override implicit lazy val app: Application = localGuiceApplicationBuilder()
    .overrides(
      bind[PayApiConnector].toInstance(mockPayConnector),
      bind[AuthJourney].toInstance(mockAuthJourney)
    )
    .build()

  def controller =
    new PaymentsController(
      mockPayConnector,
      mockAuthJourney,
      injected[WithBreadcrumbAction],
      injected[MessagesControllerComponents],
      injected[ErrorRenderer]
    )(mockLocalPartialRetriever, injected[ConfigDecorator], mock[TemplateRenderer], injected[ExecutionContext])

  when(mockAuthJourney.authWithPersonalDetails).thenReturn(new ActionBuilderFixture {
    override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] =
      block(
        buildUserRequest(
          request = request
        ))
  })

  "makePayment" should {
    "redirect to the response's nextUrl" in {

      val expectedNextUrl = "someNextUrl"
      val createPaymentResponse = CreatePayment("someJourneyId", expectedNextUrl)

      when(mockPayConnector.createPayment(any())(any(), any()))
        .thenReturn(Future.successful(Some(createPaymentResponse)))

      val result = controller.makePayment()(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("someNextUrl")
    }

    "redirect to a BAD_REQUEST page if createPayment failed" in {

      when(mockPayConnector.createPayment(any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.makePayment()(FakeRequest())
      status(result) shouldBe BAD_REQUEST
    }
  }
}
