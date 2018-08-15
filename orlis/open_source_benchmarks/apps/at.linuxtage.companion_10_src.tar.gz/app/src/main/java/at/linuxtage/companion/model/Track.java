package at.linuxtage.companion.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StringRes;

import at.linuxtage.companion.R;

public class Track implements Parcelable {

	public static enum Type {
		other(R.string.other),
		keynote(R.string.keynote),
        workshop(R.string.workshop),
        discussion(R.string.discussion),
        lecture(R.string.lecture),
        meeting(R.string.meeting),
        lightning_talk(R.string.lightning_talk),
        //devroom(R.string.developer_room),
        //maintrack(R.string.main_track),
        // certification(R.string.certification_exam),
        exam(R.string.exam);

		private final int nameResId;

		private Type(@StringRes int nameResId) {
			this.nameResId = nameResId;
		}

		public int getNameResId() {
			return nameResId;
		}
	}

	private String name;
	private Type type;

	public Track() {
	}

	public Track(String name, Type type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + name.hashCode();
		result = prime * result + type.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Track other = (Track) obj;
		return name.equals(other.name) && (type == other.type);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(name);
		out.writeInt(type.ordinal());
	}

	public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
		public Track createFromParcel(Parcel in) {
			return new Track(in);
		}

		public Track[] newArray(int size) {
			return new Track[size];
		}
	};

	private Track(Parcel in) {
		name = in.readString();
		type = Type.values()[in.readInt()];
	}
}
