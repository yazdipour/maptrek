package mobi.maptrek.io;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.WireFormat;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Track.TrackPoint;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.util.ProgressListener;

public class TrackManager extends Manager {
    public static final String EXTENSION = ".mtrack";
    private static final int FIELD_COLOR = 4;
    private static final int FIELD_NAME = 3;
    private static final int FIELD_POINT = 2;
    private static final int FIELD_POINT_ACCURACY = 6;
    private static final int FIELD_POINT_ALTITUDE = 3;
    private static final int FIELD_POINT_BEARING = 5;
    private static final int FIELD_POINT_CONTINUOUS = 8;
    private static final int FIELD_POINT_LATITUDE = 1;
    private static final int FIELD_POINT_LONGITUDE = 2;
    private static final int FIELD_POINT_SPEED = 4;
    private static final int FIELD_POINT_TIMESTAMP = 7;
    private static final int FIELD_VERSION = 1;
    private static final int FIELD_WIDTH = 5;
    public static final int VERSION = 1;

    @NonNull
    public FileDataSource loadData(InputStream inputStream, String filePath) throws Exception {
        long propertiesOffset = 0;
        Track track = new Track();
        CodedInputStream input = CodedInputStream.newInstance(inputStream);
        boolean done = false;
        while (!done) {
            long offset = (long) input.getTotalBytesRead();
            int tag = input.readTag();
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 0:
                    done = true;
                    break;
                case 1:
                    input.skipField(tag);
                    break;
                case 2:
                    int oldLimit = input.pushLimit(input.readRawVarint32());
                    readPoint(track, input);
                    input.popLimit(oldLimit);
                    input.checkLastTagWas(0);
                    break;
                case 3:
                    propertiesOffset = offset;
                    track.name = input.readBytes().toStringUtf8();
                    break;
                case 4:
                    track.style.color = input.readUInt32();
                    break;
                case 5:
                    track.style.width = input.readFloat();
                    break;
                default:
                    throw new InvalidProtocolBufferException("Unsupported proto field: " + tag);
            }
        }
        inputStream.close();
        track.id = (filePath.hashCode() * 31) + 1;
        FileDataSource dataSource = new FileDataSource();
        dataSource.name = track.name;
        dataSource.tracks.add(track);
        track.source = dataSource;
        dataSource.propertiesOffset = propertiesOffset;
        return dataSource;
    }

    public void saveData(OutputStream outputStream, FileDataSource source, @Nullable ProgressListener progressListener) throws Exception {
        if (source.tracks.size() != 1) {
            throw new Exception("Only single track can be saved in mtrack format");
        }
        Track track = (Track) source.tracks.get(0);
        if (progressListener != null) {
            progressListener.onProgressStarted(track.points.size());
        }
        CodedOutputStream output = CodedOutputStream.newInstance(outputStream);
        output.writeUInt32(1, 1);
        int progress = 0;
        for (TrackPoint point : track.points) {
            output.writeTag(2, 2);
            output.writeRawVarint32(getSerializedPointSize(point));
            output.writeInt32(1, point.latitudeE6);
            output.writeInt32(2, point.longitudeE6);
            output.writeFloat(3, point.elevation);
            output.writeFloat(4, point.speed);
            output.writeFloat(5, point.bearing);
            output.writeFloat(6, point.accuracy);
            output.writeUInt64(7, point.time);
            if (!point.continuous) {
                output.writeBool(8, point.continuous);
            }
            progress++;
            if (progressListener != null) {
                progressListener.onProgressChanged(progress);
            }
        }
        output.writeBytes(3, ByteString.copyFromUtf8(track.name));
        output.writeUInt32(4, track.style.color);
        output.writeFloat(5, track.style.width);
        output.flush();
        outputStream.close();
        if (progressListener != null) {
            progressListener.onProgressFinished();
        }
    }

    @NonNull
    public String getExtension() {
        return EXTENSION;
    }

    private void readPoint(Track track, CodedInputStream input) throws IOException {
        int latitudeE6 = 0;
        int longitudeE6 = 0;
        boolean continuous = true;
        float altitude = Float.NaN;
        float speed = Float.NaN;
        float bearing = Float.NaN;
        float accuracy = Float.NaN;
        long timestamp = 0;
        boolean done = false;
        while (!done) {
            int tag = input.readTag();
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 0:
                    done = true;
                    break;
                case 1:
                    latitudeE6 = input.readInt32();
                    break;
                case 2:
                    longitudeE6 = input.readInt32();
                    break;
                case 3:
                    altitude = input.readFloat();
                    break;
                case 4:
                    speed = input.readFloat();
                    break;
                case 5:
                    bearing = input.readFloat();
                    break;
                case 6:
                    accuracy = input.readFloat();
                    break;
                case 7:
                    timestamp = input.readUInt64();
                    break;
                case 8:
                    continuous = input.readBool();
                    break;
                default:
                    throw new InvalidProtocolBufferException("Unsupported proto field: " + tag);
            }
        }
        track.addPointFast(continuous, latitudeE6, longitudeE6, altitude, speed, bearing, accuracy, timestamp);
    }

    public int getSerializedPointSize(TrackPoint point) {
        int size = ((((((0 + CodedOutputStream.computeInt32Size(1, point.latitudeE6)) + CodedOutputStream.computeInt32Size(2, point.longitudeE6)) + CodedOutputStream.computeFloatSize(3, point.elevation)) + CodedOutputStream.computeFloatSize(4, point.speed)) + CodedOutputStream.computeFloatSize(5, point.bearing)) + CodedOutputStream.computeFloatSize(6, point.accuracy)) + CodedOutputStream.computeUInt64Size(7, point.time);
        if (point.continuous) {
            return size;
        }
        return size + CodedOutputStream.computeBoolSize(8, point.continuous);
    }

    public void saveProperties(FileDataSource source) throws Exception {
        Track track = (Track) source.tracks.get(0);
        ByteBuffer buffer = ByteBuffer.allocate(getSerializedPropertiesSize(track));
        CodedOutputStream output = CodedOutputStream.newInstance(buffer);
        output.writeBytes(3, ByteString.copyFromUtf8(track.name));
        output.writeUInt32(4, track.style.color);
        output.writeFloat(5, track.style.width);
        output.flush();
        File file = new File(source.path);
        long createTime = file.lastModified();
        RandomAccessFile access = new RandomAccessFile(file, "rw");
        access.setLength(source.propertiesOffset + 1);
        access.seek(source.propertiesOffset);
        access.write(buffer.array());
        access.close();
        file.setLastModified(createTime);
    }

    public int getSerializedPropertiesSize(Track track) {
        return ((0 + CodedOutputStream.computeStringSize(3, track.name)) + CodedOutputStream.computeUInt32Size(4, track.style.color)) + CodedOutputStream.computeFloatSize(5, track.style.width);
    }
}
