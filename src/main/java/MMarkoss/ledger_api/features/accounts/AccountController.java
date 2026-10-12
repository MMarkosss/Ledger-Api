package MMarkoss.ledger_api.features.accounts;

import MMarkoss.ledger_api.features.accounts.dto.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@AllArgsConstructor
@RequestMapping("/contas")
public class AccountController {
    private final AccountService accServ;

    @GetMapping
    public ResponseEntity<Page<AccGetRes>> getAllAcc
            (@PageableDefault(size = 20, sort = {"holderName"})Pageable pageable) {
        Page<Account> accs = accServ.listAccs(pageable);

        Page<AccGetRes> response = accs.map(AccGetRes::new);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccGetOneRes> getOneAcc(@PathVariable UUID id) {
        Account acc = accServ.findOneByIdAcc(id);

        return ResponseEntity.ok(new AccGetOneRes(acc));
    }

    @PostMapping
    public ResponseEntity<AccPostRes> postAcc (@RequestBody @Valid AccPostReq dto) {
        Account acc = accServ.createAcc(dto);

        return ResponseEntity.status(201).body(new AccPostRes(acc));
    }

    @PutMapping
    public ResponseEntity<AccPostRes> putAcc(@RequestBody @Valid AccPostReq dto) {
        Account acc = accServ.updateAcc(dto);

        return ResponseEntity.ok(new AccPostRes(acc));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AccPostRes> patchAcc (@PathVariable UUID id, @RequestBody @Valid AccPatchReq dto) {
        Account acc = accServ.partialupdate(id,dto);

        return ResponseEntity.ok(new AccPostRes(acc));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAcc (@PathVariable UUID id) {
        accServ.deleteAcc(id);

        return ResponseEntity.noContent().build();
    }
}
