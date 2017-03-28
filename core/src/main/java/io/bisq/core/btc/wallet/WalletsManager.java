/*
 * This file is part of bisq.
 *
 * bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bisq.core.btc.wallet;

import com.google.inject.Inject;
import io.bisq.common.handlers.ExceptionHandler;
import io.bisq.common.handlers.ResultHandler;
import io.bisq.core.crypto.ScryptUtil;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.wallet.DeterministicSeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import javax.annotation.Nullable;

// Convenience class to handle methods applied to several wallets
public class WalletsManager {
    private static final Logger log = LoggerFactory.getLogger(WalletsManager.class);

    private final BtcWalletService btcWalletService;
    private final TradeWalletService tradeWalletService;
    private final BsqWalletService bsqWalletService;
    private final WalletsSetup walletsSetup;

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public WalletsManager(BtcWalletService btcWalletService, TradeWalletService tradeWalletService, BsqWalletService bsqWalletService, WalletsSetup walletsSetup) {
        this.btcWalletService = btcWalletService;
        this.tradeWalletService = tradeWalletService;
        this.bsqWalletService = bsqWalletService;
        this.walletsSetup = walletsSetup;
    }

    public void decryptWallets(KeyParameter aesKey) {
        btcWalletService.decryptWallet(aesKey);
        bsqWalletService.decryptWallet(aesKey);
        tradeWalletService.setAesKey(null);
    }

    public void encryptWallets(KeyCrypterScrypt keyCrypterScrypt, KeyParameter aesKey) {
        bsqWalletService.encryptWallet(keyCrypterScrypt, aesKey);
        btcWalletService.encryptWallet(keyCrypterScrypt, aesKey);

        // we save the key for the trade wallet as we don't require passwords here
        tradeWalletService.setAesKey(aesKey);
    }

    public String getWalletsAsString(boolean includePrivKeys) {
        return "BTC Wallet:\n" +
                btcWalletService.getWalletAsString(includePrivKeys) +
                "\n\nBSQ Wallet:\n" +
                bsqWalletService.getWalletAsString(includePrivKeys);
    }

    public void restoreSeedWords(@Nullable DeterministicSeed seed, ResultHandler resultHandler, ExceptionHandler exceptionHandler) {
        walletsSetup.restoreSeedWords(seed, resultHandler, exceptionHandler);
    }

    public void backupWallets() {
        walletsSetup.backupWallets();
    }

    public void clearBackup() {
        walletsSetup.clearBackups();
    }

    public boolean areWalletsEncrypted() {
        return btcWalletService.isEncrypted() && bsqWalletService.isEncrypted();
    }

    public boolean areWalletsAvailable() {
        return btcWalletService.isWalletReady() && bsqWalletService.isWalletReady();
    }

    public KeyCrypterScrypt getKeyCrypterScrypt() {
        if (areWalletsEncrypted() && btcWalletService.getKeyCrypter() != null)
            return (KeyCrypterScrypt) btcWalletService.getKeyCrypter();
        else
            return ScryptUtil.getKeyCrypterScrypt();
    }

    public boolean checkAESKey(KeyParameter aesKey) {
        return btcWalletService.checkAESKey(aesKey);
    }

    public long getChainSeedCreationTimeSeconds() {
        return btcWalletService.getKeyChainSeed().getCreationTimeSeconds();
    }

    public boolean hasPositiveBalance() {
        return btcWalletService.getBalance(Wallet.BalanceType.AVAILABLE)
                .add(bsqWalletService.getBalance(Wallet.BalanceType.AVAILABLE))
                .isPositive();
    }

    public void setAesKey(KeyParameter aesKey) {
        btcWalletService.setAesKey(aesKey);
        bsqWalletService.setAesKey(aesKey);
        tradeWalletService.setAesKey(aesKey);
    }

    public DeterministicSeed getDecryptedSeed(KeyParameter aesKey, DeterministicSeed keyChainSeed, KeyCrypter keyCrypter) {
        if (keyCrypter != null) {
            return keyChainSeed.decrypt(keyCrypter, "", aesKey);
        } else {
            log.warn("keyCrypter is null");
            return null;
        }
    }
}