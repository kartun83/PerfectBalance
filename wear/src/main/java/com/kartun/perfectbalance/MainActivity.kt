package com.kartun.perfectbalance

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Half.EPSILON
import android.util.Log
import android.view.View
import com.kartun.perfectbalance.databinding.ActivityMainBinding
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : Activity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mSensorManager: SensorManager
    private var mAccelerometer : Sensor ?= null
    //private var mRotation : Sensor ?= null
    private var mGravity : Sensor ?= null
    private var resume = false

    //--
    private val NS2S = 1.0f / 1000000000.0f
    private val deltaRotationVector = FloatArray(4) { 0f }
    private var timestamp: Float = 0f
    //--

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        this.mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        this.mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        //this.mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    /**
     * Called when there is a new sensor event.  Note that "on changed"
     * is somewhat of a misnomer, as this will also be called if we have a
     * new reading from a sensor with the exact same sensor values (but a
     * newer timestamp).
     *
     *
     * See [SensorManager][android.hardware.SensorManager]
     * for details on possible sensor types.
     *
     * See also [SensorEvent][android.hardware.SensorEvent].
     *
     *
     * **NOTE:** The application doesn't own the
     * [event][android.hardware.SensorEvent]
     * object passed as a parameter and therefore cannot hold on to it.
     * The object may be part of an internal pool and may be reused by
     * the framework.
     *
     * @param event the [SensorEvent][android.hardware.SensorEvent].
     */
    override fun onSensorChanged(event: SensorEvent?) {
        Log.d("Sensor event", event.toString());
        if (event != null && resume) {
            when (event.sensor.type) {
                //Sensor.TYPE_ACCELEROMETER -> handleAccel(event)
                //Sensor.TYPE_GYROSCOPE -> handleGrav(event)
                Sensor.TYPE_GAME_ROTATION_VECTOR ->  handleGameRotation(event)
                //Sensor.TYPE_ROTATION_VECTOR ->  handleAccel(event)//handleGameRotation(event)
                else -> {binding.CenterField.text = event.sensor.type.toString()}
            }
        }
        else
        {
            binding.CenterField.text = "Not running"
        }
    }

    override fun onResume() {
        binding.CenterField.text = "Resume"
        super.onResume()
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        //mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        //mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL)
//        mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        binding.CenterField.text = "Pause"
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    public fun onToggleRoutine(view: android.view.View) {
        binding.CenterField.text = "Test1"
        this.resume = !this.resume;
        if (this.resume)
        {
            onResume();
        }
        else
        {
            onPause();
        }
    }
    private fun resumeReading() {
        this.resume = true
    }

    private fun pauseReading() {
        this.resume = false
    }

    /**
     * Called when the accuracy of the registered sensor has changed.  Unlike
     * onSensorChanged(), this is only called when this accuracy value changes.
     *
     *
     * See the SENSOR_STATUS_* constants in
     * [SensorManager][android.hardware.SensorManager] for details.
     *
     * @param accuracy The new accuracy of this sensor, one of
     * `SensorManager.SENSOR_STATUS_*`
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //TODO("Not yet implemented")
        return
    }

    private fun handleGameRotation(event: SensorEvent)
    {
        //SensorManager.getInclination()
        //SensorManager.getAngleChange()
        val rotationMatrix = FloatArray(9) { 0f }
        val orientation = FloatArray(3) { 0f }
        SensorManager.getRotationMatrixFromVector(rotationMatrix ,event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        //gotOrientation(mRotationAngles[0], mRotationAngles[1], mRotationAngles[2]);
        binding.CenterField.text = "GR: \nPitch: ${Math.toDegrees(event.values[1].toDouble())}\nRoll:${Math.toDegrees(event.values[2].toDouble())}\nAzimuth:${Math.toDegrees(event.values[0].toDouble())}";
    }

    private fun handleAccel(event: SensorEvent)
    {
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.

        val alpha: Float = 0.8f

        // Isolate the force of gravity with the low-pass filter.
        val gravity = FloatArray(3) { 0f }
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

        // Remove the gravity contribution with the high-pass filter.
        val linear_acceleration = FloatArray(3) { 0f }
        linear_acceleration[0] = event.values[0] - gravity[0]
        linear_acceleration[1] = event.values[1] - gravity[1]
        linear_acceleration[2] = event.values[2] - gravity[2]

        binding.CenterField.text = "Accel: \nX: ${Math.toDegrees(deltaRotationVector[0].toDouble())}\nY:${deltaRotationVector[1]}\nZ:${deltaRotationVector[2]}";
    }

    private fun handleGrav(event: SensorEvent?)
    {
// This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        if (timestamp != 0f && event != null) {
            val dT = (event.timestamp - timestamp) * NS2S
            // Axis of the rotation sample, not normalized yet.
            var axisX: Float = event.values[0]
            var axisY: Float = event.values[1]
            var axisZ: Float = event.values[2]

            // Calculate the angular speed of the sample
            val omegaMagnitude: Float = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)

            // Normalize the rotation vector if it's big enough to get the axis
            // (that is, EPSILON should represent your maximum allowable margin of error)
            if (omegaMagnitude > EPSILON) {
                axisX /= omegaMagnitude
                axisY /= omegaMagnitude
                axisZ /= omegaMagnitude
            }

            // Integrate around this axis with the angular speed by the timestep
            // in order to get a delta rotation from this sample over the timestep
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix.
            val thetaOverTwo: Float = omegaMagnitude * dT / 2.0f
            val sinThetaOverTwo: Float = sin(thetaOverTwo)
            val cosThetaOverTwo: Float = cos(thetaOverTwo)
            deltaRotationVector[0] = sinThetaOverTwo * axisX
            deltaRotationVector[1] = sinThetaOverTwo * axisY
            deltaRotationVector[2] = sinThetaOverTwo * axisZ
            deltaRotationVector[3] = cosThetaOverTwo
        }
        timestamp = event?.timestamp?.toFloat() ?: 0f
        val deltaRotationMatrix = FloatArray(9) { 0f }
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
        // User code should concatenate the delta rotation we computed with the current rotation
        // in order to get the updated rotation.
        // rotationCurrent = rotationCurrent * deltaRotationMatrix;


        //binding.CenterField.text = "X: ${deltaRotationVector[0]}\nY:${deltaRotationVector[1]}\nZ:${deltaRotationVector[2]}";
        binding.CenterField.text = "TR: \nX: ${Math.toDegrees(deltaRotationVector[0].toDouble())}\nY:${Math.toDegrees(deltaRotationVector[1].toDouble())}\nZ:${Math.toDegrees(deltaRotationVector[2].toDouble())}";
    }
}