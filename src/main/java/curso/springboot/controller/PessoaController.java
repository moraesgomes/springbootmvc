package curso.springboot.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import curso.springboot.model.Pessoa;
import curso.springboot.model.Telefone;
import curso.springboot.repository.PessoaRepository;
import curso.springboot.repository.ProfissaoRepository;
import curso.springboot.repository.TelefoneRepository;

@Controller
public class PessoaController {

	@Autowired
	private PessoaRepository pessoaRepository;

	@Autowired
	private TelefoneRepository telefoneRepository;

	@Autowired
	private ReportUtil reportUtil;

	@Autowired
	private ProfissaoRepository profissaoRepository;

	@RequestMapping(method = RequestMethod.GET, value = "/cadastropessoa")
	public ModelAndView inicio() {

		ModelAndView modelAndView = new ModelAndView("cadastro/cadastropessoa");
		modelAndView.addObject("pessoaobj", new Pessoa());
		modelAndView.addObject("pessoas", pessoaRepository.findAll(PageRequest.of(0, 5, Sort.by("nome"))));
		modelAndView.addObject("profissoes", profissaoRepository.findAll());
		return modelAndView;

	}

	@RequestMapping(method = RequestMethod.POST, value = "**/salvarpessoa", consumes = { "multipart/form-data" })
	public ModelAndView save(@Valid Pessoa pessoa, BindingResult bindingResult, final MultipartFile file)
			throws IOException {

		pessoa.setTelefones(telefoneRepository.getTelefones(pessoa.getId()));

		if (bindingResult.hasErrors()) {

			ModelAndView modelAndView = new ModelAndView("cadastro/cadastropessoa");
			modelAndView.addObject("pessoas", pessoaRepository.findAll(PageRequest.of(0, 5, Sort.by("nome"))));
			modelAndView.addObject("pessoaobj", pessoa);

			List<String> msg = new ArrayList<String>();

			for (ObjectError objectError : bindingResult.getAllErrors()) {

				msg.add(objectError.getDefaultMessage()); // Vem das anotações inseridas na minha classe de modelo

			}

			modelAndView.addObject("msg", msg);
			modelAndView.addObject("profissoes", profissaoRepository.findAll());
			return modelAndView;
		}

		if (file.getSize() > 0) {

			pessoa.setCurriculo(file.getBytes());
			pessoa.setTipoFileCurriculo(file.getContentType());
			pessoa.setNomeFileCurriculo(file.getOriginalFilename());
		} else {

			if (pessoa.getId() != null && pessoa.getId() > 0) { // editando pessoa

				Pessoa pessoaTemp = pessoaRepository.findById(pessoa.getId()).get();
				pessoa.setCurriculo(pessoaTemp.getCurriculo());
				pessoa.setTipoFileCurriculo(pessoaTemp.getTipoFileCurriculo());
				pessoa.setNomeFileCurriculo(pessoaTemp.getNomeFileCurriculo());
			}
		}

		pessoaRepository.save(pessoa);

		ModelAndView andView = new ModelAndView("cadastro/cadastropessoa");
		andView.addObject("pessoas", pessoaRepository.findAll(PageRequest.of(0, 5, Sort.by("nome"))));
		andView.addObject("pessoaobj", new Pessoa());

		return andView;

	}

	@RequestMapping(method = RequestMethod.GET, value = "/listapessoas")
	public ModelAndView pessoas() {

		ModelAndView andView = new ModelAndView("cadastro/cadastropessoa");
		andView.addObject("pessoas", pessoaRepository.findAll(PageRequest.of(0, 5, Sort.by("nome"))));
		andView.addObject("pessoaobj", new Pessoa());

		return andView;
	}

	@GetMapping("/editarpessoa/{idpessoa}")
	public ModelAndView editar(@PathVariable("idpessoa") Long idpessoa) {

		Optional<Pessoa> pessoa = pessoaRepository.findById(idpessoa);
		ModelAndView modelAndView = new ModelAndView("cadastro/cadastropessoa");
		modelAndView.addObject("pessoaobj", pessoa.get());
		modelAndView.addObject("profissoes", profissaoRepository.findAll());

		return modelAndView;

	}

	@GetMapping("/removerpessoa/{idpessoa}")
	public ModelAndView excluir(@PathVariable("idpessoa") Long idpessoa) {

		pessoaRepository.deleteById(idpessoa);
		ModelAndView modelAndView = new ModelAndView("cadastro/cadastropessoa");
		modelAndView.addObject("pessoas", pessoaRepository.findAll(PageRequest.of(0, 5, Sort.by("nome"))));
		modelAndView.addObject("pessoaobj", new Pessoa());

		return modelAndView;

	}

	@GetMapping("**baixarcurriculo/{idpessoa}")
	public void baixarcurriculo(@PathVariable("idpessoa") Long idpessoa, HttpServletResponse response)
			throws IOException {

		// Consultar o objeto pessoa no banco de dados

		Pessoa pessoa = pessoaRepository.findById(idpessoa).get();

		if (pessoa.getCurriculo() != null) {

			// Setar o tamnho da reposta
			response.setContentLength(pessoa.getCurriculo().length);

			// Tipo do arquivo para download ou pode ser genérica usando
			// application/octet-stream
			response.setContentType(pessoa.getTipoFileCurriculo());

			// Define o cabeçalho da resposta

			String headerKey = "Content/Disposition";
			String headerValue = String.format("attachemnt; filename=\"%s\"", pessoa.getNomeFileCurriculo());
			response.setHeader(headerKey, headerValue);

			// Finaliza a reposta passando o arquivo

			response.getOutputStream().write(pessoa.getCurriculo());

		}
	}

	@PostMapping("**/pesquisarpessoa")
	public ModelAndView pesquisar(@RequestParam("nomepesquisa") String nomepesquisa,
			@RequestParam("pesquisasexo") String pesquisasexo) {

		List<Pessoa> pessoas = new ArrayList<Pessoa>();

		if (pesquisasexo != null && !pesquisasexo.isEmpty()) {

			pessoas = pessoaRepository.findPessoaByNameSexo(nomepesquisa, pesquisasexo);
		} else {

			pessoas = pessoaRepository.findPessoaByName(nomepesquisa);
		}

		ModelAndView modelAndView = new ModelAndView("cadastro/cadastropessoa");
		modelAndView.addObject("pessoas", pessoas);
		modelAndView.addObject("pessoaobj", new Pessoa());

		return modelAndView;

	}

	@GetMapping("**/pesquisarpessoa")
	public void imprimePdf(@RequestParam("nomepesquisa") String nomepesquisa,
			@RequestParam("pesquisasexo") String pesquisasexo, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		List<Pessoa> pessoas = new ArrayList<Pessoa>();

		if (pesquisasexo != null && !pesquisasexo.isEmpty() && nomepesquisa != null && !nomepesquisa.isEmpty()) {

			pessoas = pessoaRepository.findPessoaByNameSexo(nomepesquisa, pesquisasexo);

		} else if (nomepesquisa != null && !nomepesquisa.isEmpty()) {

			pessoas = pessoaRepository.findPessoaByName(nomepesquisa);

		}

		else if (pesquisasexo != null && !pesquisasexo.isEmpty()) {

			pessoas = pessoaRepository.findPessoaBySexo(pesquisasexo);

		}

		else {

			Iterable<Pessoa> iterator = pessoaRepository.findAll();

			for (Pessoa pessoa : iterator) {

				pessoas.add(pessoa);

			}
		}

		// Chamar o serviço que gera o relatório

		byte[] pdf = reportUtil.gerarRelatorio(pessoas, "pessoa", request.getServletContext());

		// Tamanho da resposta para o navegador

		response.setContentLength(pdf.length);

		// definir o tipo de arquivo na resposta

		response.setContentType("application/octet-stream");

		// Definir o cabeçalho da resposta

		String headerKey = "Content-Disposition";
		String headerValue = String.format("attachment; filename=\"%s\"", "relatoriopessoa.pdf");
		response.setHeader(headerKey, headerValue);

		// Finaliza a resposta para o navegador

		response.getOutputStream().write(pdf);

	}

	@GetMapping("/telefones/{idpessoa}")
	public ModelAndView telefones(@PathVariable("idpessoa") Long idpessoa) {

		Optional<Pessoa> pessoa = pessoaRepository.findById(idpessoa);
		ModelAndView modelAndView = new ModelAndView("cadastro/telefones");
		modelAndView.addObject("pessoaobj", pessoa.get());

		modelAndView.addObject("telefones", telefoneRepository.getTelefones(idpessoa));

		return modelAndView;

	}

	@PostMapping("**/addfonePessoa/{pessoaid}")
	public ModelAndView addFonePessoa(Telefone telefone, @PathVariable("pessoaid") Long pessoaid) {

		Pessoa pessoa = pessoaRepository.findById(pessoaid).get();

		if (telefone != null && telefone.getNumero().isEmpty() || telefone.getTipo().isEmpty()) {

			ModelAndView modelAndView = new ModelAndView("cadastro/telefones");
			modelAndView.addObject("pessoaobj", pessoa);
			modelAndView.addObject("telefones", telefoneRepository.getTelefones(pessoaid));

			List<String> msg = new ArrayList<String>();
			if (telefone.getNumero().isEmpty()) {

				msg.add("Número deve ser informado");

			}

			if (telefone.getTipo().isEmpty()) {

				msg.add("Tipo deve ser informado");
			}

			modelAndView.addObject("msg", msg);

			return modelAndView;
		}

		telefone.setPessoa(pessoa);
		telefoneRepository.save(telefone);

		ModelAndView modelAndView = new ModelAndView("cadastro/telefones");
		modelAndView.addObject("pessoaobj", pessoa);
		modelAndView.addObject("telefones", telefoneRepository.getTelefones(pessoaid));

		return modelAndView;
	}

	@GetMapping("/removertelefone/{idtelefone}")
	public ModelAndView excluirTelefone(@PathVariable("idtelefone") Long idtelefone) {

		Pessoa pessoa = telefoneRepository.findById(idtelefone).get().getPessoa();

		telefoneRepository.deleteById(idtelefone);

		ModelAndView modelAndView = new ModelAndView("cadastro/telefones");
		modelAndView.addObject("pessoaobj", pessoa);

		modelAndView.addObject("telefones", telefoneRepository.getTelefones(pessoa.getId()));

		return modelAndView;

	}

}
