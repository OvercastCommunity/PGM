package tc.oc.util.translations;

import java.util.logging.Logger;
import tc.oc.util.translations.provider.TranslationProvider;

public final class Translations extends BaseTranslator {

  private static Translations instance;

  private Translations() {
    super(Logger.getGlobal(), new TranslationProvider("strings"));
  }

  public static Translations get() {
    return instance == null ? instance = new Translations() : instance;
  }
}
