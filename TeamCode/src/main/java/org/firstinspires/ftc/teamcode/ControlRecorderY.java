package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import org.firstinspires.ftc.robotcontroller.internal.Log;

import java.util.ArrayList;
import java.util.List;

@TeleOp(name="ControlRecorderY", group="Linear Opmode")
public class ControlRecorderY extends LinearOpMode {

    private DcMotor frontLeft, frontRight, backLeft, backRight, chainDrive, frontArm;
    private CRServo armRotator, dropper;
    private Log logger;

    private int upperLimit = -45;
    private int lowerLimit = -130;
    private double armSpeed = 0.75;
    private int armPosition;

    private List<ControlData> controlDataList = new ArrayList<>();
    private boolean replayMode = false;
    private int replayIndex = 0;
    private long replayStartTime;

    private static class ControlData {
        long timestamp;
        double frontLeftPower, frontRightPower, backLeftPower, backRightPower;
        double armPower, chainPower, armRotatorPower, dropperPower;
        int armPosition;

        ControlData(long timestamp, double fl, double fr, double bl, double br,
                    double armP, double chainP, double armRotatorP, double dropperP, int armPos) {
            this.timestamp = timestamp;
            frontLeftPower = fl;
            frontRightPower = fr;
            backLeftPower = bl;
            backRightPower = br;
            armPower = armP;
            chainPower = chainP;
            armRotatorPower = armRotatorP;
            dropperPower = dropperP;
            armPosition = armPos;
        }
    }

    @Override
    public void runOpMode() {
        // Initialize hardware and logger
        initializeHardware();
        logger = new Log("teleop_log", true);

        waitForStart();
        replayStartTime = System.nanoTime();

        while (opModeIsActive()) {
            if (!replayMode) {
                // Process gamepad inputs
                processInputs();

                // Log inputs
                logInputs();

                // Start replay if 'Y' is pressed
                if (gamepad1.y) {
                    startReplay();
                }
            } else {
                // Replay recorded inputs
                replayControls();
            }

            // Update telemetry
            telemetry.update();
        }

        // Close the logger when done
        logger.close();
    }

    private void initializeHardware() {
        frontLeft = hardwareMap.get(DcMotor.class, "front_left_motor");
        frontRight = hardwareMap.get(DcMotor.class, "front_right_motor");
        backLeft = hardwareMap.get(DcMotor.class, "back_left_motor");
        backRight = hardwareMap.get(DcMotor.class, "back_right_motor");
        chainDrive = hardwareMap.get(DcMotor.class, "chain_drive");
        frontArm = hardwareMap.get(DcMotor.class, "front_arm_motor");
        armRotator = hardwareMap.get(CRServo.class, "arm_rotator_servo");
        dropper = hardwareMap.get(CRServo.class, "drop_servo");

        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);
        frontArm.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        frontArm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontArm.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    private void processInputs() {
        double drive = -gamepad1.left_stick_y;
        double strafe = gamepad1.left_stick_x;
        double rotate = gamepad1.right_stick_x;
        double frontLeftPower = drive + strafe + rotate;
        double frontRightPower = drive - strafe - rotate;
        double backLeftPower = drive - strafe + rotate;
        double backRightPower = drive + strafe - rotate;

        armPosition = frontArm.getCurrentPosition();
        double armPower = (gamepad1.right_trigger - gamepad1.left_trigger) * armSpeed;
        if ((armPosition >= upperLimit && gamepad1.right_trigger > 0)
                || (armPosition <= lowerLimit && gamepad1.left_trigger > 0)) {
            armPower = 0;
        }

        double chainPower = gamepad2.right_trigger - gamepad2.left_trigger;
        double armRotatorPower = gamepad2.left_stick_y;
        double dropperPower = gamepad2.right_stick_y;

        frontLeft.setPower(frontLeftPower);
        frontRight.setPower(frontRightPower);
        backLeft.setPower(backLeftPower);
        backRight.setPower(backRightPower);
        frontArm.setPower(armPower);
        chainDrive.setPower(chainPower);
        armRotator.setPower(armRotatorPower);
        dropper.setPower(dropperPower);

        // Record the control inputs
        long timestamp = System.nanoTime() - replayStartTime;
        controlDataList.add(new ControlData(timestamp, frontLeftPower, frontRightPower, backLeftPower, backRightPower,
                armPower, chainPower, armRotatorPower, dropperPower, armPosition));
    }

    private void logInputs() {
        // Capture the current time and data, then log it
        long timestamp = System.nanoTime() - replayStartTime;
        logger.addData("Timestamp", timestamp);
        logger.addData("FL", frontLeft.getPower());
        logger.addData("FR", frontRight.getPower());
        logger.addData("BL", backLeft.getPower());
        logger.addData("BR", backRight.getPower());
        logger.addData("Arm", frontArm.getPower());
        logger.addData("Chain", chainDrive.getPower());
        logger.addData("Rotator", armRotator.getPower());
        logger.addData("Dropper", dropper.getPower());
        logger.addData("Arm Position", armPosition);

        // Update the log file with the recorded data
        logger.update();
    }

    private void startReplay() {
        replayMode = true;
        replayIndex = 0;
        replayStartTime = System.nanoTime(); // Reset the start time for replay
    }

    private void replayControls() {
        if (replayIndex < controlDataList.size()) {
            ControlData data = controlDataList.get(replayIndex);

            // Calculate the elapsed time since replay started
            long currentTime = System.nanoTime();
            long elapsedTime = currentTime - replayStartTime;

            // Check if we should apply the next set of saved inputs
            if (elapsedTime >= data.timestamp) {
                // Set motors and servos to recorded values
                frontLeft.setPower(data.frontLeftPower);
                frontRight.setPower(data.frontRightPower);
                backLeft.setPower(data.backLeftPower);
                backRight.setPower(data.backRightPower);
                frontArm.setPower(data.armPower);
                chainDrive.setPower(data.chainPower);
                armRotator.setPower(data.armRotatorPower);
                dropper.setPower(data.dropperPower);

                // Increment the replay index to the next set of saved inputs
                replayIndex++;
            }
        } else {
            // Replay finished, stop all motion and exit replay mode
            stopAllMotion();
            replayMode = false;
            telemetry.addData("Replay", "Finished");
        }
    }

    private void stopAllMotion() {
        frontLeft.setPower(0);
        frontRight.setPower(0);
        backLeft.setPower(0);
        backRight.setPower(0);
        frontArm.setPower(0);
        chainDrive.setPower(0);
        armRotator.setPower(0);
        dropper.setPower(0);
    }
}