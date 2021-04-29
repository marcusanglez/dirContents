package com.qohash.dirContents;

import com.qohash.dirContents.internal.DirListerTotalSize;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SpringBootApplication
@RestController
public class DirContentsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DirContentsApplication.class, args);
	}

	/**
	 * The web API is in the form of [server]:[port]/displayContents?dir=[encodedDir]
	 * @param dir needs to be encoded
	 * @return a List of strings with the content of the file
	 */
	@GetMapping("/displayContents")
	public List<String> displayContents(@RequestParam(value="dir", defaultValue = "/home/marco/dev")String dir) {
		DirListerTotalSize dirListerTotalSize = new DirListerTotalSize();
		try {
			return dirListerTotalSize.getAllDirElements(dir);
		} catch (Exception e) {
			return List.of(e.getCause().toString());
		}
	}
}
