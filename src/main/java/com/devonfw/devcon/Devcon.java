package com.devonfw.devcon;

import com.devonfw.devcon.common.api.CommandRegistry;
import com.devonfw.devcon.common.impl.CommandManagerImpl;
import com.devonfw.devcon.common.impl.CommandRegistryImpl;
import com.devonfw.devcon.input.ConsoleInput;
import com.devonfw.devcon.input.ConsoleInputManager;
import com.devonfw.devcon.input.Input;
import com.devonfw.devcon.output.ConsoleOutput;
import com.devonfw.devcon.output.Output;
import com.github.zafarkhaja.semver.Version;

/**
 * Main class of DevCon
 *
 * @author pparrado
 */
public class Devcon {

  public static final String VERSION = "0.9.0";

  public static final String VERSION_URL = "http://localhost:3000/version";

  public static final Version VERSION_ = Version.valueOf(VERSION);

  public static final String DEVCON_VERSION = "devcon v." + VERSION;

  public static final String DEVCON_BANNER = "Hello, this is Devcon!\n" + "Copyright (c) 2016 Capgemini";

  /**
   * @param args command line arguments
   */
  public static void main(String[] args) {

    System.out.println(DEVCON_BANNER);

    Input input = new ConsoleInput(System.in, System.out);
    Output output = new ConsoleOutput(System.out);
    CommandRegistry registry = new CommandRegistryImpl("com.devonfw.devcon.modules.*");

    ConsoleInputManager inputmanager = new ConsoleInputManager(new CommandManagerImpl(registry, input, output));
    inputmanager.parse(args);
  }

}
