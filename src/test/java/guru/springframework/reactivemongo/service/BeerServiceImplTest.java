package guru.springframework.reactivemongo.service;

import guru.springframework.reactivemongo.domain.Beer;
import guru.springframework.reactivemongo.mappers.BeerMapper;
import guru.springframework.reactivemongo.mappers.BeerMapperImpl;
import guru.springframework.reactivemongo.model.BeerDTO;
import guru.springframework.reactivemongo.repository.BeerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest
public class BeerServiceImplTest {

    @Container
    @ServiceConnection
    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest");

    @Autowired
    private BeerService beerService;

    @Autowired
    private BeerMapper beerMapper;

    @Autowired
    private BeerRepository beerRepository;

    BeerDTO beerDTO;

    @BeforeEach
    void setUp() {
        beerDTO = beerMapper.beerToBeerDTO(getTestBeer());
    }

    @Test
    void findFirstByBeerName() {
        BeerDTO savedBeerDto = getSavedBeerDto();
        AtomicBoolean savedBeerExists = new AtomicBoolean(false);

        Mono<BeerDTO> firstByBeerName = beerService.findFirstByBeerName(savedBeerDto.getBeerName());

        firstByBeerName.subscribe(dto -> {
            System.out.println(dto.toString());
            savedBeerExists.set(true);
        });

        await().untilTrue(savedBeerExists);
    }

    @Test
    void findFirstByBeerStyle() {
        BeerDTO savedBeerDto = getSavedBeerDto();
        AtomicBoolean savedBeerExists = new AtomicBoolean(false);

        beerService.findByBeerStyle(savedBeerDto.getBeerStyle()).subscribe(dto -> {
            System.out.println(dto.toString());
            savedBeerExists.set(true);
        });

        await().untilTrue(savedBeerExists);
    }

    @Test
    @DisplayName("Test Save Beer Using Subscriber")
    void saveBeer() {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        Mono<BeerDTO> savedBeerMono = beerService.saveBeer(Mono.just(beerDTO));

        savedBeerMono.subscribe(savedBeer -> {
            System.out.println("Beer saved successfully :" + savedBeer.getId());
            atomicBoolean.set(true);
        });

        await().untilTrue(atomicBoolean);
    }

    @Test
    @DisplayName("Test Save Beer Using Block")
    void testSaveBeerUseBlock() {
        BeerDTO savedDto = beerService.saveBeer(Mono.just(getTestBeerDTO())).block();
        assertThat(savedDto).isNotNull();
        assertThat(savedDto.getId()).isNotNull();
    }

    @Test
    @DisplayName("Test Update Beer Using Block")
    void testUpdateBlocking() {
        final String newName = "New Beer Name";
        BeerDTO savedBeerDto = getSavedBeerDto();
        savedBeerDto.setBeerName(newName);

        BeerDTO updatedDto = beerService.saveBeer(Mono.just(savedBeerDto)).block();

        BeerDTO fetchedDto = beerService.getById(updatedDto.getId()).block();
        assertThat(fetchedDto.getBeerName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("Test Update Using Reactive Streams")
    void testUpdateStreaming() {
        final String newName = "New Beer Name";

        AtomicReference<BeerDTO> atomicDto = new AtomicReference<>();

        beerService.saveBeer(Mono.just(getTestBeerDTO()))
                .map(savedBeerDto -> {
                    savedBeerDto.setBeerName(newName);
                    return savedBeerDto;
                })
                .flatMap(beerService::saveBeer)
                .flatMap(savedUpdatedDto -> beerService.getById(savedUpdatedDto.getId())) // get from db
                .subscribe(atomicDto::set);

        await().until(() -> atomicDto.get() != null);
        assertThat(atomicDto.get().getBeerName()).isEqualTo(newName);
    }

    @Test
    void testDeleteBeer() {
        BeerDTO beerToDelete = getSavedBeerDto();

        beerService.deleteBeer(beerToDelete.getId()).block();

        Mono<BeerDTO> expectedEmptyBeerMono = beerService.getById(beerToDelete.getId());

        BeerDTO emptyBeer = expectedEmptyBeerMono.block();

        assertThat(emptyBeer).isNull();

    }

    public static Beer getTestBeer() {
        return Beer.builder()
                .beerName("Space Dust")
                .beerStyle("IPA")
                .price(BigDecimal.TEN)
                .quantityOnHand(12)
                .upc("123213")
                .build();
    }

    public static BeerDTO getTestBeerDTO() {
        return new BeerMapperImpl().beerToBeerDTO(getTestBeer());
    }

    public BeerDTO getSavedBeerDto() {
        return beerService.saveBeer(Mono.just(getTestBeerDTO())).block();
    }

}