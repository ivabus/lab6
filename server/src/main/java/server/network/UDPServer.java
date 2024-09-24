package server.network;

import common.network.requests.Request;
import common.network.responses.Response;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationException;
import org.apache.logging.log4j.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.SerializationUtils;
import server.App;

import common.network.responses.NoSuchCommandResponse;
import server.handlers.CommandHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * UDP обработчик запросов
 * @author L1mPeX
 */
abstract class UDPServer {
  private final InetSocketAddress addr;
  private final CommandHandler commandHandler;
  private Runnable afterHook;

  private final Logger logger = App.logger;

  private boolean running = true;

  public UDPServer(InetSocketAddress addr, CommandHandler commandHandler) {
    this.addr = addr;
    this.commandHandler = commandHandler;
  }

  public InetSocketAddress getAddr() {
    return addr;
  }

  /**
   * Получает данные с клиента.
   * Возвращает пару из данных и адреса клиента
   */
  public abstract Pair<Byte[], SocketAddress> receiveData() throws IOException;

  /**
   * Отправляет данные клиенту
   */
  public abstract void sendData(byte[] data, SocketAddress addr) throws IOException;

  public abstract void connectToClient(SocketAddress addr) throws SocketException;

  public abstract void disconnectFromClient();
  public abstract void close();

  public void run() {
    logger.info("Сервер запущен по адресу " + addr);

    while (running) {
      Pair<Byte[], SocketAddress> dataPair;
      try {
        dataPair = receiveData();
      } catch (Exception e) {
        logger.error("Ошибка получения данных : " + e.toString(), e);
        disconnectFromClient();
        continue;
      }

      var dataFromClient = dataPair.getKey();
      var clientAddr = dataPair.getValue();

      try {
        connectToClient(clientAddr);
        logger.info("Соединено с " + clientAddr);
      } catch (Exception e) {
        logger.error("Ошибка соединения с клиентом : " + e.toString(), e);
      }

      Request request;
      try {
        request = SerializationUtils.deserialize(ArrayUtils.toPrimitive(dataFromClient));
        logger.info("Обработка " + request + " из " + clientAddr);
      } catch (SerializationException e) {
        logger.error("Невозможно десериализовать объект запроса.", e);
        disconnectFromClient();
        continue;
      }

      Response response = null;
      try {
        response = commandHandler.handle(request);
        if (afterHook != null) afterHook.run();
      } catch (Exception e) {
        logger.error("Ошибка выполнения команды : " + e.toString(), e);
      }
      if (response == null) response = new NoSuchCommandResponse(request.getName());

      var data = SerializationUtils.serialize(response);
      logger.info("Ответ: " + response);

      try {
        sendData(data, clientAddr);
        logger.info("Отправлен ответ клиенту " + clientAddr);
      } catch (Exception e) {
        logger.error("Ошибка ввода-вывода : " + e.toString(), e);
      }

      disconnectFromClient();
      logger.info("Отключение от клиента " + clientAddr);
    }

    close();
  }

  /**
   * Вызывает хук после каждого запроса.
   * @param afterHook хук, вызываемый после каждого запроса
   */
  public void setAfterHook(Runnable afterHook) {
    this.afterHook = afterHook;
  }

  public void stop() {
    running = false;
  }
}
