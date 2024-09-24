package common.exceptions;

/**
 * Выбрасывается, если в ответе сервера ошибка
 * @author L1mPeX
 */
public class APIException extends Exception {
  public APIException(String message) {
    super(message);
  }
}
