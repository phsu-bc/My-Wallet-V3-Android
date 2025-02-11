package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.models.data.CryptoWithdrawalFeeAndLimit
import com.blockchain.testutils.lumens
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.erc20.Erc20NonCustodialAccount
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase
import java.math.BigInteger
import kotlin.test.assertEquals

class TradingToOnChainTxEngineTest : CoincoreTestBase() {

    private val isNoteSupported = false
    private val walletManager: CustodialWalletManager = mock()

    private val subject = TradingToOnChainTxEngine(
        walletManager = walletManager,
        isNoteSupported = isNoteSupported
    )

    @Before
    fun setup() {
        initMocks()
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount = mockSourceAccount()
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        // Assert
        verify(txTarget).asset
        verify(sourceAccount).asset

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Asset incorrect`() {
        val sourceAccount = mock<Erc20NonCustodialAccount> {
            on { asset }.thenReturn(WRONG_ASSET)
        }

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        // Assert
        verify(txTarget).asset
        verify(sourceAccount).asset

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `asset is returned correctly`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.sourceAsset

        // Assert
        assertEquals(asset, ASSET)
        verify(sourceAccount).asset

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        // Arrange
        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        val feesAndLimits = CryptoWithdrawalFeeAndLimit(minLimit = 5000.toBigInteger(), fee = BigInteger.ONE)
        whenever(walletManager.fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.BUY))
            .thenReturn(Single.just(feesAndLimits))

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(txTarget.asset) &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance ==
                    actionableBalance.minus(CryptoValue.fromMinor(txTarget.asset, feesAndLimits.fee)) &&
                    it.feeForFullAvailable == CryptoValue.fromMinor(txTarget.asset, feesAndLimits.fee) &&
                    it.feeAmount == CryptoValue.fromMinor(txTarget.asset, feesAndLimits.fee) &&
                    it.selectedFiat == TEST_USER_FIAT &&
                    it.confirmations.isEmpty() &&
                    it.minLimit == CryptoValue.fromMinor(ASSET, feesAndLimits.minLimit) &&
                    it.maxLimit == null &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        // Arrange
        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        val feesAndLimits = CryptoWithdrawalFeeAndLimit(minLimit = 5000.toBigInteger(), fee = BigInteger.ONE)
        whenever(walletManager.fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.BUY))
            .thenReturn(Single.just(feesAndLimits))

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(txTarget.asset),
            totalBalance = totalBalance,
            availableBalance = actionableBalance.minus(
                CryptoValue.fromMinor(txTarget.asset, feesAndLimits.fee)
            ),
            feeForFullAvailable = CryptoValue.fromMinor(txTarget.asset, feesAndLimits.fee),
            feeAmount = CryptoValue.fromMinor(txTarget.asset, feesAndLimits.fee),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        val inputAmount = 2.lumens()

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance ==
                    actionableBalance.minus(CryptoValue.fromMinor(txTarget.asset, feesAndLimits.fee))
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertComplete()
            .assertNoErrors()

        verify(sourceAccount, atLeastOnce()).asset
        verify(sourceAccount).accountBalance
        verify(sourceAccount).actionableBalance
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from NONE to PRIORITY is rejected`() {
        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()
        val inputAmount = 2.lumens()
        val zeroPax = 0.lumens()

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = actionableBalance,
            feeAmount = zeroPax,
            feeForFullAvailable = zeroPax,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Priority,
            -1
        ).test()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from NONE to REGULAR is rejected`() {
        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()
        val inputAmount = 2.lumens()
        val zeroPax = 0.lumens()

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = actionableBalance,
            feeForFullAvailable = zeroPax,
            feeAmount = zeroPax,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Regular,
            -1
        ).test()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from NONE to CUSTOM is rejected`() {
        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()
        val inputAmount = 2.lumens()
        val zeroPax = 0.lumens()

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = actionableBalance,
            feeForFullAvailable = zeroPax,
            feeAmount = zeroPax,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Custom,
            100
        ).test()
    }

    @Test
    fun `update fee level from NONE to NONE has no effect`() {
        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()
        val inputAmount = 2.lumens()
        val zeroPax = 0.lumens()

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = actionableBalance,
            feeForFullAvailable = zeroPax,
            feeAmount = zeroPax,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.None,
            -1
        ).test()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == actionableBalance &&
                    it.feeAmount == zeroPax
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertComplete()
            .assertNoErrors()

        noMoreInteractions(sourceAccount, txTarget)
    }

    private fun mockSourceAccount(
        totalBalance: Money = CryptoValue.zero(ASSET),
        availableBalance: Money = CryptoValue.zero(ASSET)
    ) = mock<Erc20NonCustodialAccount> {
        on { asset }.thenReturn(ASSET)
        on { accountBalance }.thenReturn(Single.just(totalBalance))
        on { actionableBalance }.thenReturn(Single.just(availableBalance))
    }

    private fun verifyFeeLevels(feeSelection: FeeSelection) =
        feeSelection.selectedLevel == FeeLevel.None &&
            feeSelection.availableLevels == setOf(FeeLevel.None) &&
            feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
            feeSelection.asset == null &&
            feeSelection.customAmount == -1L

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(sourceAccount)
        verifyNoMoreInteractions(walletManager)
        verifyNoMoreInteractions(currencyPrefs)
        verifyNoMoreInteractions(exchangeRates)
    }

    companion object {
        private val ASSET = CryptoCurrency.XLM
        private val WRONG_ASSET = CryptoCurrency.BTC
    }
}
