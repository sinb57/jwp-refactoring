package kitchenpos.application.menu;

import kitchenpos.application.menu.dto.request.MenuRequestAssembler;
import kitchenpos.application.menu.dto.request.product.ProductRequest;
import kitchenpos.application.menu.dto.response.MenuResponseAssembler;
import kitchenpos.application.menu.dto.response.ProductResponse;
import kitchenpos.domain.menu.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final MenuRequestAssembler requestAssembler;
    private final MenuResponseAssembler responseAssembler;

    public ProductService(final ProductRepository productRepository,
                          final MenuRequestAssembler requestAssembler,
                          final MenuResponseAssembler responseAssembler
    ) {
        this.productRepository = productRepository;
        this.requestAssembler = requestAssembler;
        this.responseAssembler = responseAssembler;
    }

    @Transactional
    public ProductResponse create(final ProductRequest request) {
        final var product = requestAssembler.asProduct(request);
        final var savedProduct = productRepository.save(product);
        return responseAssembler.asProductResponse(savedProduct);
    }

    public List<ProductResponse> list() {
        final var products = productRepository.findAll();
        return responseAssembler.asProductResponses(products);
    }
}
