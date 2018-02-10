package pattern

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import common._
import org.scalatest.{Matchers, WordSpecLike}
import pattern.AccountBalanceResponseHandler._

import scala.concurrent.duration._

class CameoSpec extends TestKit(ActorSystem("CameoTestAS")) with ImplicitSender with WordSpecLike with Matchers {
  "An AccountBalanceRetriever" should {
    "return a list of account balances" in {
      val probe1 = TestProbe()
      val probe2 = TestProbe()
      val savingsAccountsProxy = system.actorOf(Props[SavingsAccountsProxyStub], "cameo-success-savings")
      val checkingAccountsProxy = system.actorOf(Props[CheckingAccountsProxyStub], "cameo-success-checkings")
      val moneyMarketAccountsProxy = system.actorOf(Props[MoneyMarketAccountsProxyStub], "cameo-success-money-markets")
      val accountBalanceRetriever = system.actorOf(Props(new AccountBalanceRetriever(savingsAccountsProxy, checkingAccountsProxy, moneyMarketAccountsProxy)), "cameo-retriever1")

      within(300 milliseconds) {
        probe1.send(accountBalanceRetriever, GetCustomerAccountBalances(1L))
        val result = probe1.expectMsgType[AccountBalances]
        result should equal(AccountBalances(Some(List((3, 15000))), Some(List((1, 150000), (2, 29000))), Some(List())))
      }
      within(300 milliseconds) {
        probe2.send(accountBalanceRetriever, GetCustomerAccountBalances(2L))
        val result = probe2.expectMsgType[AccountBalances]
        result should equal(AccountBalances(Some(List((6, 640000), (7, 1125000), (8, 40000))), Some(List((5, 80000))), Some(List((9, 640000), (10, 1125000), (11, 40000)))))
      }
    }

    "return a TimeoutException when timeout is exceeded" in {
      val savingsAccountsProxy = system.actorOf(Props[TimingOutSavingsAccountProxyStub], "cameo-timing-out-savings")
      val checkingAccountsProxy = system.actorOf(Props[CheckingAccountsProxyStub], "cameo-timing-out-checkings")
      val moneyMarketAccountsProxy = system.actorOf(Props[MoneyMarketAccountsProxyStub], "cameo-timing-out-money-markets")
      val accountBalanceRetriever = system.actorOf(Props(new AccountBalanceRetriever(savingsAccountsProxy, checkingAccountsProxy, moneyMarketAccountsProxy)), "cameo-timing-out-retriever")
      val probe = TestProbe()

      within(250 milliseconds, 500 milliseconds) {
        probe.send(accountBalanceRetriever, GetCustomerAccountBalances(1L))
        probe.expectMsg(AccountRetrievalTimeout)
      }
    }
  }
}