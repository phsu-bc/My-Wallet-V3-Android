package piuk.blockchain.androidcore.data.settings

import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.rxjava3.core.Observable
import okhttp3.ResponseBody
import piuk.blockchain.androidcore.utils.annotations.WebRequest

class SettingsService(private val settingsApi: SettingsManager) {

    /**
     * Fetches a new [Settings] object from the server and returns it as an Observable.
     * @return An [<] containing the user's settings
     */
    fun getSettingsObservable(): Observable<Settings> =
        Observable.defer { getSettings() }

    /**
     * Fetches the latest [Settings] object for the user.
     *
     * @return An [Settings] object for the current user
     */
    @WebRequest
    internal fun getSettings(): Observable<Settings> = settingsApi.info

    /**
     * Initializes the [SettingsManager] with the user's GUID and SharedKey.
     *
     * @param guid The user's GUID
     * @param sharedKey The shared key
     */
    internal fun initSettings(guid: String, sharedKey: String) {
        settingsApi.initSettings(guid, sharedKey)
    }

    /**
     * Update the user's email
     *
     * @param email The email to be stored
     * @return A [ResponseBody] containing the response from the server
     */
    @WebRequest
    internal fun updateEmail(email: String): Observable<ResponseBody> =
        settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_EMAIL, email)

    @WebRequest
    internal fun updateEmail(email: String, context: String?): Observable<ResponseBody> =
        settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_EMAIL, email, context)

    /**
     * Update the user's phone number
     *
     * @param sms The phone number to be stored
     * @return A [ResponseBody] containing the response from the server
     */
    @WebRequest
    internal fun updateSms(sms: String): Observable<ResponseBody> =
        settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_SMS, sms)

    /**
     * Verify the user's phone number with a verification code
     *
     * @param code The verification code
     * @return A [ResponseBody] containing the response from the server
     */
    @WebRequest
    internal fun verifySms(code: String): Observable<ResponseBody> =
        settingsApi.updateSetting(SettingsManager.METHOD_VERIFY_SMS, code)

    /**
     * Update the user's Tor blocking preference
     *
     * @param blocked The user's preference for blocking Tor requests
     * @return A [ResponseBody] containing the response from the server
     */
    @WebRequest
    internal fun updateTor(blocked: Boolean): Observable<ResponseBody> = settingsApi.updateSetting(
        SettingsManager.METHOD_UPDATE_BLOCK_TOR_IPS,
        if (blocked) 1 else 0
    )

    /**
     * Enable or disable a specific notification type for a user/
     *
     * @param notificationType The type of notification to enable
     * @return A [ResponseBody] containing the response from the server
     * @see Settings
     */
    @WebRequest
    internal fun updateNotifications(notificationType: Int): Observable<ResponseBody> =
        settingsApi.updateSetting(
            SettingsManager.METHOD_UPDATE_NOTIFICATION_TYPE,
            notificationType
        )

    /**
     * Enable or disable all notifications
     *
     * @param enable Whether or not to enable notifications
     * @return An [<] containing the response from the server
     * @see Settings
     */
    @WebRequest
    internal fun enableNotifications(enable: Boolean): Observable<ResponseBody> =
        settingsApi.updateSetting(
            SettingsManager.METHOD_UPDATE_NOTIFICATION_ON,
            if (enable) SettingsManager.NOTIFICATION_ON else SettingsManager.NOTIFICATION_OFF
        )

    /**
     * Update the user's two factor status
     *
     * @param authType The auth type being used for 2FA
     * @return A [ResponseBody] containing the response from the server
     * @see Settings
     */
    @WebRequest
    internal fun updateTwoFactor(authType: Int): Observable<ResponseBody> =
        settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_AUTH_TYPE, authType)

    /**
     * Update the user's fiat unit preference
     *
     * @param fiatUnit The user's preference for fiat unit
     * @return A [ResponseBody] containing the response from the server
     */
    @WebRequest
    internal fun updateFiatUnit(fiatUnit: String): Observable<ResponseBody> =
        settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_CURRENCY, fiatUnit)

    @WebRequest
    internal fun updateLastTxTime(epochTime: String): Observable<ResponseBody> =
        settingsApi.updateSetting(SettingsManager.METHOD_UPDATE_LAST_TX_TIME, epochTime)
}
