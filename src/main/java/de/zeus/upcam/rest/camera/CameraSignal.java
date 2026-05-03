package de.zeus.upcam.rest.camera;

public class CameraSignal {
    private static final CameraSignal NONE = new CameraSignal(false, false, false, false);

    private final boolean motionDetected;
    private final boolean personDetected;
    private final boolean vehicleDetected;
    private final boolean animalDetected;

    public CameraSignal(boolean motionDetected, boolean personDetected, boolean vehicleDetected, boolean animalDetected) {
        this.motionDetected = motionDetected;
        this.personDetected = personDetected;
        this.vehicleDetected = vehicleDetected;
        this.animalDetected = animalDetected;
    }

    public static CameraSignal none() {
        return NONE;
    }

    public boolean isMotionDetected() {
        return motionDetected;
    }

    public boolean isPersonDetected() {
        return personDetected;
    }

    public boolean isVehicleDetected() {
        return vehicleDetected;
    }

    public boolean isAnimalDetected() {
        return animalDetected;
    }

    public boolean isAnyActive() {
        return motionDetected || personDetected || vehicleDetected || animalDetected;
    }

    public String summarize() {
        return "md=" + flag(motionDetected)
                + ",person=" + flag(personDetected)
                + ",vehicle=" + flag(vehicleDetected)
                + ",animal=" + flag(animalDetected);
    }

    private String flag(boolean value) {
        return value ? "1" : "0";
    }
}
