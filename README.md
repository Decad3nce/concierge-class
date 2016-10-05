# concierge-class

Concierge handles your parcels and makes sure they get marshalled and unmarshalled correctly when crossing IPC boundaries even when there is a version mismatch between the client protocol level and the RPC implementation.

The receive and prepare pattern allows you to nest Parcelable classes without having to worry about expected length, size, and contents of the parcel aside from reassigning to local variables in your class.

An example of a multiple protocol release Parcelable is given below.

### Incoming #receiveParcel
On incoming parcel (to be unmarshalled) as part of you from parcel definition in a given Parcelable class.
```java
ParcelInfo incomingParcelInfo = Concierge.receiveParcel(incomingParcel);
int parcelableVersion = incomingParcelInfo.getParcelVersion();
 
// Do unmarshalling steps here iterating over every known version
// until you reach your original protocol definition for a given
// parcelable.
if (parcelableVersion >= ReleasedVersions.TWO) {
    this.resourcesPackageName = parcel.readString();
    this.collapsePanel = (parcel.readInt() == 1);
    if (parcel.readInt() != 0) {
        this.remoteIcon = Bitmap.CREATOR.createFromParcel(parcel);
    }
    if (parcel.readInt() != 0) {
        this.deleteIntent = PendingIntent.CREATOR.createFromParcel(parcel);
    }
    this.sensitiveData = (parcel.readInt() == 1);
}
 
if (parcelableVersion >= ReleasedVersions.ONE) {
    if (parcel.readInt() != 0) {
        this.onLongClick = PendingIntent.CREATOR.createFromParcel(parcel);
    }
}
 
// Complete the process
incomingParcelInfo.complete();
```

### Outgoing #prepareParcel
On outgoing parcel (to be marshalled) as part of your writeToParcel definition in a given
Parcelable class.
```java 
ParcelInfo outgoingParcelInfo = Concierge.prepareParcel(incomingParcel);
 
// Do marshalling steps here iterating over every known version you've released
// ==== ReleasedVersions.ONE =====
out.writeString(resourcesPackageName);
out.writeInt(collapsePanel ? 1 : 0);
if (remoteIcon != null) {
    out.writeInt(1);
    remoteIcon.writeToParcel(out, 0);
} else {
    out.writeInt(0);
}
if (deleteIntent != null) {
    out.writeInt(1);
    deleteIntent.writeToParcel(out, 0);
} else {
    out.writeInt(0);
}
out.writeInt(sensitiveData ? 1 : 0);
 
// ==== ReleasedVersions.TWO ====
if (onLongClick != null) {
    out.writeInt(1);
    onLongClick.writeToParcel(out, 0);
} else {
    out.writeInt(0);
}
// Complete the process
outgoingParcelInfo.complete();
```
