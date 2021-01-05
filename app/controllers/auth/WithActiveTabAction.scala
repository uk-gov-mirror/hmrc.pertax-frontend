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

package controllers.auth

import controllers.auth.requests.UserRequest
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}
import uk.gov.hmrc.renderer.ActiveTab
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

class WithActiveTabAction @Inject()(implicit ec: ExecutionContext) {

  def addActiveTab(currentActiveTab: ActiveTab): ActionRefiner[UserRequest, UserRequest] =
    new ActionRefiner[UserRequest, UserRequest] {
      override protected def refine[A](request: UserRequest[A]): Future[Either[Result, UserRequest[A]]] =
        Future.successful(
          Right(
            UserRequest(
              request.nino,
              request.retrievedName,
              request.saUserType,
              request.credentials,
              request.confidenceLevel,
              request.personDetails,
              request.trustedHelper,
              request.profile,
              request.unreadMessageCount,
              Some(currentActiveTab),
              request.breadcrumb,
              request.request
            )
          )
        )

      override protected def executionContext: ExecutionContext = ec
    }
}
