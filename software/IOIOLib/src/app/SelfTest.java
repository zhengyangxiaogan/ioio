package app;

import ioio.lib.Input;
import ioio.lib.Ioio;
import ioio.lib.IoioException;
import ioio.lib.IoioException.ConnectionLostException;
import ioio.lib.IoioException.OutOfResourceException;
import ioio.lib.IoioFactory;
import ioio.lib.Output;
import ioio.lib.PwmOutput;
import ioio.lib.pic.IoioLogger;
import ioio.lib.pic.Uart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * High level tests and example of usage for the IOIOLib
 *
 * @author arshan
 */
public class SelfTest extends Activity {

	// Dont stop testing if there is a failure, try them all.
	private static final boolean FORCE_ALL_TESTS = false;

	// Some setup for the tests, this pins should be shorted to each other
	public static final int OUTPUT_PIN = 10;
	public static final int INPUT_PIN = 11;
	public static final int ANALOG_INPUT_PIN = 33;
	public static final int ANALOG_OUTPUT_PIN = 14;

	// note that these overload the above digital i/o connections, but input/output reversed
	public static final int UART_RX = 10;
	public static final int UART_TX = 11;

	// for repetitive tests, do this many
	public static final int REPETITIONS = 5;

    private static final int PWM_OUT_PIN = 7;

	// UI, sort of.
	private LinearLayout layout_root;
	private TextView messageText;
	private TextView statusText;
	private TextView titleText;

    Ioio ioio;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setupViews();
    	ioio = IoioFactory.makeIoio();
    	// TODO(arshan): intercept Log.i output and put to screen
    	// TODO(arshan): buttons for restart/pause/nextTest
    }

    @Override
    public void onStart() {
    	super.onStart();

    	// Start a thread to run the tests.
    	new Thread() {
    		@Override
            public void run() {
    			try {
    			    
    				status("Connecting");
                    ioio.waitForConnect();
    				testConnection();

                    status("Testing");

                    /*
    				testHardReset();
     			    testSoftReset();

    			    testDisconnectReconnect();

    				// should test hard reset too.
    			    //testDigitalOutput(); // for probing output with meter
     				testDigitalIO();
                    testAnalogInput();
                    // testPWM();
                    // testServo();
                      
                     
                     */
     				testUart(); 
     				
    				msg("Tests Finished");

    				if (FORCE_ALL_TESTS) {
    					status("FORCED", Color.YELLOW);
    				}
    				else {
    					status("PASSED", Color.GREEN);
    				}

    			} catch (FailException fail) {
    				IoioLogger.log("failed"); // to get timing info in logs
    				fail.printStackTrace();
    				status("FAILED", Color.RED);
    				for (StackTraceElement line : fail.getStackTrace()) {
    					msg(line.toString());
    				}
    			} catch (Exception e) {
    				exception(e);
    				e.printStackTrace();
    			} finally {
//    			    ioio.disconnect();
    			}
			}
		}.start();

    }

    public void testConnection() throws FailException {
    	msg("Connecting to IOIO");
    	assertTrue(ioio.isConnected());
    }

    public void testHardReset() throws FailException {
    	msg("Starting Hard Reset Test");
    	for (int x = 0; x < REPETITIONS; x++) {
			try {
                ioio.hardReset();
            } catch (ConnectionLostException e) {
                exception(e);
            }
			IoioLogger.log("hard reset complete");
			sleep(500);
			try {
                ioio.waitForConnect();
            } catch (IoioException e) {
                e.printStackTrace();
                IoioLogger.log("exception in hard reset");
                exception(e);
            }
			assertTrue(ioio.isConnected());
		}
    }

    /**
     * How fast and how often can we soft reset.
     * @throws FailException
     */
    public void testSoftReset() throws FailException {
    	msg("Starting Soft Reset Test");
		for (int x = 0; x < REPETITIONS; x++) {
			try {
                ioio.softReset();
            } catch (ConnectionLostException e) {
                e.printStackTrace();
                exception(e);
            }
			sleep(500);
			try {
                ioio.waitForConnect();
            } catch (IoioException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                IoioLogger.log("soft reset failed");
                assertFalse(true);
            }
			assertTrue(ioio.isConnected());
		}
    }

    /**
     * Example of using a digital output, slow so that can be measured with a meter
     * @throws FailException
     */
    public void testDigitalOutput() throws FailException {
    	msg("Starting Digital Output Test");
    	Output<Boolean> output = null;
    	try {
            output = ioio.openDigitalOutput(OUTPUT_PIN, false);
    		for (int x = 0; x < REPETITIONS; x++) {
    			output.write(true);
    			sleep(500);
    			assertTrue(output.getLastWrittenValue());
    			output.write(false);
    			sleep(500);
    			assertFalse(output.getLastWrittenValue());
    		}
    		output.close();
    	} catch (IoioException e) {
    		exception(e);
    	} catch (IOException ioe) {
    	    exception(ioe);
    	}
    }

    /**
     * Example of using a Digital Input
     * @throws FailException
     */
    public void testDigitalIO() throws FailException {
    	msg("Starting Digital I/O Test");
    	try {
            ioio.softReset();
        } catch (ConnectionLostException e) {
            e.printStackTrace();
            exception(e);
        }
    	sleep(1000); // wait for soft reset? debugging
        try {
        	ioio.waitForConnect();
        	Input<Boolean> input = ioio.openDigitalInput(INPUT_PIN);
        	Output<Boolean> output = ioio.openDigitalOutput(OUTPUT_PIN, false);
        	sleep(100);
			for (int x = 0; x < REPETITIONS; x++) {
				output.write(!output.getLastWrittenValue());
				sleep(100); // experimentally seems to take a bit more then 75mS
				IoioLogger.log("doing input compare"); // to get timing info in the log
				assertEquals(output.getLastWrittenValue(), input.read());
			}
			input.close();
			output.close();
		} catch (IoioException e) {
            exception(e);
        } catch (IOException e) {
            exception(e);
        }
    }

    public void testAnalogInput() throws FailException {
    	msg("Starting Analog Input Test");
        try {
            ioio.softReset();
        } catch (ConnectionLostException e) {
            e.printStackTrace();
            exception(e);
        }
        sleep(1000); // wait for soft reset? debugging
        try {
            ioio.waitForConnect();
            boolean bit = false;
            Output<Boolean> output = ioio.openDigitalOutput(ANALOG_OUTPUT_PIN, bit);
            Input<Float> input = ioio.openAnalogInput(ANALOG_INPUT_PIN);
            sleep(100);
			for (int x = 0; x < REPETITIONS; x++) {
				sleep(100);
				bit = !output.getLastWrittenValue();
                output.write(bit);
				sleep(200);
                IoioLogger.log("analog pins : [" + bit + "] " + input.read());
				assertTrue(bit ? input.read() > 0.9f : input.read() < 0.1f);
			}
			output.close();
			input.close();
        } catch (IoioException e) {
            exception(e);
        } catch (IOException e) {
            exception(e);
        }
    }

    public void testServo() throws FailException {
        msg("Starting Servo Test");
        
        
    }
    
    public void testUart() throws FailException {
    	msg("Starting UART Test");
    	testUartAtBaud(Uart.BAUD_9600);
    	softReset(); // BUG? is this cause SW doesnt tear down or FW? 
    	testUartAtBaud(Uart.BAUD_19200);
    	softReset();
    	testUartAtBaud(Uart.BAUD_38400);    	  
    }

    public void softReset() {
        try {
            ioio.softReset();
        } catch (ConnectionLostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        sleep(100);
    }
    protected void testUartAtBaud(int baud) {
    
        boolean cache_test = false;
        msg("Testing UART at " + baud);
        try {
        	// TODO(arshan): try this at different settings?
        	Uart uart = ioio.openUart(UART_RX, UART_TX,
        			baud, Uart.NO_PARITY, Uart.ONE_STOP_BIT );
        	InputStream in = uart.openInputStream();
        	OutputStream out = uart.openOutputStream();
        	// TODO(arshan): do the corner case chars too
        	int c;
        	String TEST = "The quick red fox jumped over the lazy grey dog; Also !@#$%^*()_+=-`~\\|][{}";
        	for (int x = 0; x < TEST.length(); x++) {
    			out.write(TEST.charAt(x));
    			c = in.read();
    			assertTrue(c == TEST.charAt(x));
        	}
        	msg("passed inline test");
        	
        	if (cache_test) {
        	// now without blocking ... tests the caching
        	for (int x = 0; x < TEST.length(); x++) {
                out.write(TEST.charAt(x));
            }
        	msg("all bytes in cache");
        	for (int x = 0; x < TEST.length(); x++) {
                c = in.read();
                assertTrue(c == TEST.charAt(x));
            }
        	msg("passed cached test");
        	}
        	
        	uart.close();
    	} catch (Exception e) {
    	    e.printStackTrace();
    	    exception(e);
    	}
    }

    private void testDisconnectReconnect() throws FailException {
        msg("Starting disconnect/connect test");
        ioio.disconnect();
        IoioLogger.log("disconnected");
        assertFalse(ioio.isConnected());
        sleep(1000);
        try {
            ioio.waitForConnect();
            IoioLogger.log("connected");
        } catch (IoioException e) {
            e.printStackTrace();
            IoioLogger.log("operation aborted");
        }
        assertTrue(ioio.isConnected());
    }

    private void testPWM() throws FailException {
        msg("Starting PWM tests");
        PwmOutput pwmOutput = null;
        final int SLEEP_TIME = 500;
        try {
            ioio.waitForConnect();
            // 10ms / 100Hz for the servo.
             pwmOutput = ioio.openPwmOutput(PWM_OUT_PIN, false, 100);
            msg("Moving right");
            for (int i = 0; i <= 5; i++) {
                pwmOutput.setDutyCycle((15 + i) / 100.f);
                msg("Increasing speed");
                sleep(SLEEP_TIME);
            }
            for (int i = 5; i > 0; i--) {
                pwmOutput.setDutyCycle((15 + i) / 100.f);
                msg("Decreasing speed");
                sleep(SLEEP_TIME);
            }
            msg("Moving left");
            for (int i = 0; i >= -5; i--) {
                pwmOutput.setDutyCycle((15 + i) / 100.f);
                sleep(SLEEP_TIME);
                msg("increasing speed");
            }
            for (int i = -4; i <= 0; i++) {
                pwmOutput.setDutyCycle((15 + i) / 100.f);
                sleep(SLEEP_TIME);
                msg("decreasing speed");
            }
            status("stopped");
        } catch (OutOfResourceException e) {
            exception(e);
        } catch (IoioException e) {
            exception(e);
        } finally {
            if (pwmOutput != null) {
                try {
                    pwmOutput.close();
                } catch (IOException e) {
                   exception(e);
                }
            }
        }
    }

    /*
     * Utility methods below.
     */
    private void sleep(int ms) {
    	try {
    		Thread.yield();
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }

    private void setupViews() {
    	LinearLayout layout = new LinearLayout(this);
    	layout.setOrientation(LinearLayout.VERTICAL);
    	layout_root = layout;

    	titleText = new TextView(this);
    	titleText.setTextSize(20);
    	titleText.setText("IOIO Self Test");

    	statusText = new TextView(this);
    	statusText.setTextSize(15);

    	messageText = new TextView(this);
    	messageText.setTextSize(12);
    	ScrollView scrolling = new ScrollView(this);
    	scrolling.addView(messageText);

    	layout.addView(titleText);
    	layout.addView(statusText);
    	layout.addView(scrolling);

    	setContentView(layout);
    	layout.setVisibility(LinearLayout.VISIBLE);
    	layout.requestFocus();
    }



    private void msg(final String txt) {
        IoioLogger.log(txt);
    	runOnUiThread(
    			new Runnable() {
    				@Override
                    public void run() {
    					messageText.append(txt+"\n");
    				}
    			});
    }

    private void status(String txt) {
    	status(txt, Color.WHITE);
    }

    private void status(final String txt, final int color) {
    	runOnUiThread(
    			new Runnable() {
    				@Override
                    public void run() {
    					// more dramatic, maybe just for fail?
    					//if (color != Color.WHITE) {
    					//	layout_root.setBackgroundColor(color);
    					//}
    				    statusText.setTextColor(color);
    					statusText.setText(txt);
    				}
    			});
    }

    private void error(String txt) {
    	status(txt, Color.RED);
    }

    private void exception(Exception e) {
    	status("Exception", Color.BLUE);
    	msg(e.toString());
    }

    private void assertTrue(boolean val) throws FailException{
    	if (!val) {
    		if (FORCE_ALL_TESTS) {
    			status("FAILED", Color.RED);
    			msg("FAILED");
    		}
    		else {
    			throw new FailException();
    		}
    	}
//    	msg(val?"pass":"fail");
    }

    private void assertFalse(boolean val) throws FailException {
        assertTrue(!val);
    }

    private void assertEquals(boolean x, boolean y) throws FailException {
    	if (x!=y) {
    		msg("not equal : " + x + " vs. " + y);
    		throw new FailException();
    	}
    }

    private class FailException extends Exception {}
}