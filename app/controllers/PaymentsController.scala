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

import com.google.inject.Inject
import config.ConfigDecorator
import connectors.PayApiConnector
import controllers.auth.{AuthJourney, WithBreadcrumbAction}
import error.ErrorRenderer
import models.{NonFilerSelfAssessmentUser, PaymentRequest, SelfAssessmentUser}
import org.joda.time.DateTime
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.CurrentTaxYear
import util.LocalPartialRetriever

import scala.concurrent.ExecutionContext

class PaymentsController @Inject()(
  val payApiConnector: PayApiConnector,
  authJourney: AuthJourney,
  withBreadcrumbAction: WithBreadcrumbAction,
  cc: MessagesControllerComponents,
  errorRenderer: ErrorRenderer)(
  implicit partialRetriever: LocalPartialRetriever,
  configDecorator: ConfigDecorator,
  val templateRenderer: TemplateRenderer,
  ec: ExecutionContext)
    extends PertaxBaseController(cc) with CurrentTaxYear {

  override def now: () => DateTime = () => DateTime.now()

  def makePayment: Action[AnyContent] =
    (authJourney.authWithPersonalDetails andThen withBreadcrumbAction.addBreadcrumb(baseBreadcrumb)).async {
      implicit request =>
        if (request.isSa) {
          request.saUserType match {
            case saUser: SelfAssessmentUser => {
              val paymentRequest = PaymentRequest(configDecorator, saUser.saUtr.toString())
              for {
                response <- payApiConnector.createPayment(paymentRequest)
              } yield {
                response match {
                  case Some(createPayment) => Redirect(createPayment.nextUrl)
                  case None                => errorRenderer.error(BAD_REQUEST)
                }
              }
            }
            case NonFilerSelfAssessmentUser => {
              Logger.warn("User had no sa account when one was required")
              errorRenderer.futureError(INTERNAL_SERVER_ERROR)
            }
          }
        } else {
          Logger.warn("User had no sa account when one was required")
          errorRenderer.futureError(INTERNAL_SERVER_ERROR)
        }
    }
}
