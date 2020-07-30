package cz.stodva.hlaseninastupu.objects;

public class VersionResponse {
    private float lastVersion;
    private String message;

    public VersionResponse() {}

    public VersionResponse(float lastVersion, String message) {
        this.lastVersion = lastVersion;
        this.message = message;
    }

    public float getVersion() {
        return lastVersion;
    }

    public void setVersion(float lastVersion) {
        this.lastVersion = lastVersion;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
