package server.commands;

import common.network.requests.*;
import common.network.responses.*;
import server.repositories.ProductRepository;

/**
 * Команда 'remove_by_id'. Удаляет элемент из коллекции.
 * @author L1mPeX
 */
public class RemoveById extends Command {
  private final ProductRepository productRepository;

  public RemoveById(ProductRepository productRepository) {
    super("remove_by_id <ID>", "удалить элемент из коллекции по ID");
    this.productRepository = productRepository;
  }

  /**
   * Выполняет команду
   * @return Успешность выполнения команды.
   */
  @Override
  public Response apply(Request request) {
    var req = (RemoveByIdRequest) request;

    try {
      if (!productRepository.checkExist(req.id)) {
        return new RemoveByIdResponse("Продукта с таким ID в коллекции нет!");
      }

      productRepository.remove(req.id);
      return new RemoveByIdResponse(null);
    } catch (Exception e) {
      return new RemoveByIdResponse(e.toString());
    }
  }
}
