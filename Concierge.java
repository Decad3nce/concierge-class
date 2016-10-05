/**
 * Copyright (C) 2016 Adnan Begovic
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package decad3nce.utils;

import android.os.Parcel;

/**
 * Concierge handles your parcels and makes sure they get marshalled and unmarshalled
 * correctly when crossing IPC boundaries even when there is a version mismatch between the client
 * protocol level and the RPC implementation.
 *
 * <p>On incoming parcel (to be unmarshalled):
 *
 * <pre class="prettyprint">
 *     ParcelInfo incomingParcelInfo = Concierge.receiveParcel(incomingParcel);
 *     int parcelableVersion = incomingParcelInfo.getParcelVersion();
 *
 *     // Do unmarshalling steps here iterating over every known version
 *     // until you reach your original protocol definition for a given
 *     // parcelable.
 *     if (parcelableVersion >= ReleasedVersions.TWO) {
 *           this.resourcesPackageName = parcel.readString();
 *           this.collapsePanel = (parcel.readInt() == 1);
 *           if (parcel.readInt() != 0) {
 *               this.remoteIcon = Bitmap.CREATOR.createFromParcel(parcel);
 *           }
 *           if (parcel.readInt() != 0) {
 *               this.deleteIntent = PendingIntent.CREATOR.createFromParcel(parcel);
 *           }
 *           this.sensitiveData = (parcel.readInt() == 1);
 *       }
 *
 *       if (parcelableVersion >= ReleasedVersions.ONE) {
 *           if (parcel.readInt() != 0) {
 *               this.onLongClick = PendingIntent.CREATOR.createFromParcel(parcel);
 *           }
 *       }
 *
 *     // Complete the process
 *     incomingParcelInfo.complete();
 * </pre>
 *
 * <p>On outgoing parcel (to be marshalled):
 *
 * <pre class="prettyprint">
 *     ParcelInfo outgoingParcelInfo = Concierge.prepareParcel(incomingParcel);
 *
 *     // Do marshalling steps here iterating over every known version
 *     // ==== ReleasedVersions.ONE =====
 *     out.writeString(resourcesPackageName);
 *     out.writeInt(collapsePanel ? 1 : 0);
 *     if (remoteIcon != null) {
 *         out.writeInt(1);
 *         remoteIcon.writeToParcel(out, 0);
 *     } else {
 *         out.writeInt(0);
 *     }
 *     if (deleteIntent != null) {
 *         out.writeInt(1);
 *         deleteIntent.writeToParcel(out, 0);
 *     } else {
 *         out.writeInt(0);
 *     }
 *     out.writeInt(sensitiveData ? 1 : 0);
 *
 *     // ==== ReleasedVersions.TWO ====
 *     if (onLongClick != null) {
 *         out.writeInt(1);
 *         onLongClick.writeToParcel(out, 0);
 *     } else {
 *         out.writeInt(0);
 *     }
 *     // Complete the process
 *     outgoingParcelInfo.complete();
 * </pre>
 *
 * The protocol version is defined statically as {@link #PARCELABLE_VERSION} and should
 * be incremented monotonically when the fundemental IPC protocol is changed.
 */
public final class Concierge {

    /** Not instantiable */
    private Concierge() {
        // Don't instantiate
    }

    /**
     * The default PARCELABLE_VERSION that defines the protocol level for outbound
     * and inbound IPC connections. The given version should be incremented monotonically
     * when the protocol is changed.
     */
    public static final int PARCELABLE_VERSION = 1;

    /**
     * Tell the concierge to receive our parcel, so we can get information from it.
     *
     * MUST CALL {@link ParcelInfo#complete()} AFTER UNMARSHALLING.
     *
     * @param parcel Incoming parcel to be unmarshalled
     * @return {@link ParcelInfo} containing parcel information, specifically the version.
     */
    public static ParcelInfo receiveParcel(Parcel parcel) {
        return new ParcelInfo(parcel);
    }

    /**
     * Prepare a parcel for the Concierge.
     *
     * MUST CALL {@link ParcelInfo#complete()} AFTER MARSHALLING.
     *
     * @param parcel Outgoing parcel to be marshalled
     * @return {@link ParcelInfo} containing parcel information, specifically the version.
     */
    public static ParcelInfo prepareParcel(Parcel parcel) {
        return new ParcelInfo(parcel, PARCELABLE_VERSION);
    }

    /**
     * Parcel header info specific to the Parcel object that is passed in via
     * {@link #prepareParcel(Parcel)} or {@link #receiveParcel(Parcel)}. The exposed method
     * of {@link #getParcelVersion()} gets the protocol level of the parcel object.
     */
    public final static class ParcelInfo {
        private Parcel mParcel;
        private int mParcelableVersion;
        private int mParcelableSize;
        private int mStartPosition;
        private int mSizePosition;
        private boolean mCreation = false;

        ParcelInfo(Parcel parcel) {
            mCreation = false;
            mParcel = parcel;
            mParcelableVersion = parcel.readInt();
            mParcelableSize = parcel.readInt();
            mStartPosition = parcel.dataPosition();
        }

        ParcelInfo(Parcel parcel, int parcelableVersion) {
            mCreation = true;
            mParcel = parcel;
            mParcelableVersion = parcelableVersion;

            // Write parcelable version, make sure to define explicit changes
            // within {@link #PARCELABLE_VERSION);
            mParcel.writeInt(mParcelableVersion);

            // Inject a placeholder that will store the parcel size from this point on
            // (not including the size itself).
            mSizePosition = parcel.dataPosition();
            mParcel.writeInt(0);
            mStartPosition = parcel.dataPosition();
        }

        /**
         * Get the parcel version from the {@link Parcel} received by the Concierge.
         * @return {@link #PARCELABLE_VERSION} of the {@link Parcel}
         */
        public int getParcelVersion() {
            return mParcelableVersion;
        }

        /**
         * Complete the {@link ParcelInfo} for the Concierge.
         */
        public void complete() {
            if (mCreation) {
                // Go back and write size
                mParcelableSize = mParcel.dataPosition() - mStartPosition;
                mParcel.setDataPosition(mSizePosition);
                mParcel.writeInt(mParcelableSize);
                mParcel.setDataPosition(mStartPosition + mParcelableSize);
            } else {
                mParcel.setDataPosition(mStartPosition + mParcelableSize);
            }
        }
    }
}
