"""Reproduz o PDF acadêmico usando os metadados locais fornecidos por argumentos."""

import argparse
import re
from html import escape
from pathlib import Path

from reportlab.graphics.shapes import Circle, Drawing, Ellipse, Line, Polygon, Rect, String
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import cm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate, Frame, KeepTogether, LongTable, PageBreak, PageTemplate,
    Paragraph, Preformatted, Spacer, TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents

ROOT = Path(__file__).resolve().parents[1]
INK = colors.HexColor("#202020")
EDGE = colors.HexColor("#555555")
PALE = colors.HexColor("#F0F0F0")
WHITE = colors.white
WIDTH = A4[0] - 5 * cm
FONT = "RelatorioTimes"
BOLD = "RelatorioTimesBold"
ITALIC = "RelatorioTimesItalic"
MONO = "RelatorioConsolas"


def fontes(diretorio):
    for nome, arquivo in (
        (FONT, "times.ttf"), (BOLD, "timesbd.ttf"), (ITALIC, "timesi.ttf"),
        (MONO, "consola.ttf"),
    ):
        pdfmetrics.registerFont(TTFont(nome, str(diretorio / arquivo)))
    pdfmetrics.registerFontFamily(FONT, normal=FONT, bold=BOLD, italic=ITALIC)


def estilos():
    base = ParagraphStyle("Texto", fontName=FONT, fontSize=12, leading=18,
                          alignment=TA_JUSTIFY, firstLineIndent=1.25 * cm,
                          spaceAfter=5, textColor=INK, allowWidows=False, allowOrphans=False)
    return {
        "body": base,
        "main": ParagraphStyle("Seção", parent=base, fontName=BOLD, firstLineIndent=0,
                               spaceBefore=0, spaceAfter=13, keepWithNext=True),
        "sub": ParagraphStyle("Subseção", parent=base, fontName=BOLD, firstLineIndent=0,
                              spaceBefore=10, spaceAfter=6, keepWithNext=True),
        "center": ParagraphStyle("Centralizado", parent=base, firstLineIndent=0,
                                 alignment=TA_CENTER),
        "title": ParagraphStyle("Título", parent=base, fontName=BOLD, firstLineIndent=0,
                                alignment=TA_CENTER, spaceAfter=8),
        "nature": ParagraphStyle("Natureza", parent=base, leftIndent=WIDTH * .48,
                                 firstLineIndent=0, leading=13.5),
        "cell": ParagraphStyle("Célula", parent=base, firstLineIndent=0,
                               alignment=0, fontSize=10.3, leading=12.4),
        "caption": ParagraphStyle("Legenda", parent=base, firstLineIndent=0,
                                  alignment=0, fontSize=10.3, leading=13, spaceAfter=4),
        "table_caption": ParagraphStyle("Título de quadro", parent=base, firstLineIndent=0,
                                        alignment=0, fontSize=10.3, leading=13, spaceAfter=4,
                                        keepWithNext=True),
        "reference": ParagraphStyle("Referência", parent=base, firstLineIndent=0,
                                    alignment=0, leading=13.5, spaceAfter=11),
        "code": ParagraphStyle("Código", fontName=MONO, fontSize=9.5, leading=12,
                               leftIndent=.45 * cm, spaceBefore=6, spaceAfter=10),
    }


def inline(texto):
    texto = escape(texto, quote=True)
    texto = re.sub(r"\*\*(.*?)\*\*", r"<b>\1</b>", texto)
    texto = re.sub(r"`([^`]+)`", rf'<font name="{MONO}" size="10">\1</font>', texto)
    return re.sub(r"\[([^\]]+)\]\((https://[^)]+)\)", r'<link href="\2" color="#202020">\1</link>', texto)


class Relatorio(BaseDocTemplate):
    def afterFlowable(self, flowable):
        entrada = getattr(flowable, "sumario", None)
        if entrada is None:
            return
        nivel, titulo, chave = entrada
        if chave == "secao-1":
            self.pagina_textual = self.page
        self.canv.bookmarkPage(chave)
        self.canv.addOutlineEntry(titulo, chave, level=nivel, closed=False)
        self.notify("TOCEntry", (nivel, escape(titulo), self.page - 1, chave))


def numero_pagina(canvas, doc):
    inicio = getattr(doc, "pagina_textual", None)
    if inicio is not None and doc.page >= inicio:
        canvas.setFillColor(INK)
        canvas.setFont(FONT, 10)
        canvas.drawRightString(A4[0] - 2 * cm, A4[1] - 2 * cm, str(doc.page - 1))


def texto(desenho, x, y, linha, tamanho=9, ancora="middle", fonte=FONT):
    desenho.add(String(x, y, linha, fontName=fonte, fontSize=tamanho,
                       textAnchor=ancora, fillColor=INK))


def caixa(desenho, x, y, largura, altura, titulo, detalhes=(), fonte=9.5):
    desenho.add(Rect(x, y, largura, altura, rx=3, ry=3, fillColor=WHITE,
                     strokeColor=EDGE, strokeWidth=.75))
    if not detalhes:
        texto(desenho, x + largura / 2, y + altura / 2 - 3, titulo, fonte, fonte=BOLD)
        return
    texto(desenho, x + largura / 2, y + altura - 13, titulo, fonte, fonte=BOLD)
    desenho.add(Line(x, y + altura - 18, x + largura, y + altura - 18,
                     strokeColor=EDGE, strokeWidth=.45))
    for i, linha in enumerate(detalhes):
        texto(desenho, x + 5, y + altura - 30 - i * 10, linha, 8.6, "start")


def linha(desenho, x1, y1, x2, y2, pontilhada=False):
    linha_ = Line(x1, y1, x2, y2, strokeColor=EDGE, strokeWidth=.75)
    if pontilhada:
        linha_.strokeDashArray = [3, 3]
    desenho.add(linha_)


def seta(desenho, x1, y1, x2, y2, pontilhada=False):
    linha(desenho, x1, y1, x2, y2, pontilhada)
    tamanho = ((x2 - x1) ** 2 + (y2 - y1) ** 2) ** .5
    dx = (x2 - x1) / tamanho
    dy = (y2 - y1) / tamanho
    desenho.add(Polygon([x2, y2, x2 - 5 * dx - 2.4 * dy, y2 - 5 * dy + 2.4 * dx,
                         x2 - 5 * dx + 2.4 * dy, y2 - 5 * dy - 2.4 * dx],
                        fillColor=EDGE, strokeColor=EDGE, strokeWidth=.3))


def ator(desenho, x, y, rotulo, papel):
    desenho.add(Circle(x, y + 19, 4.5, strokeColor=EDGE, fillColor=WHITE, strokeWidth=.85))
    for ponto in ((x, y + 14, x, y + 1), (x - 8, y + 9, x + 8, y + 9),
                  (x, y + 1, x - 7, y - 6), (x, y + 1, x + 7, y - 6)):
        linha(desenho, *ponto)
    texto(desenho, x, y - 17, rotulo, 9.5, fonte=BOLD)
    texto(desenho, x, y - 28, papel, 8.5)


def casos():
    d = Drawing(WIDTH, 217)
    d.add(Rect(83, 10, 287, 203, fillColor=WHITE, strokeColor=EDGE, strokeWidth=.85))
    texto(d, 226, 196, "API Raízes do Nordeste", 10.5, fonte=BOLD)
    vinculos = [
        (47, 173, 101, 165), (47, 121, 101, 162), (47, 121, 101, 105),
        (47, 121, 105, 132), (105, 132, 224, 132), (224, 132, 251, 111),
        (47, 51, 101, 47), (404, 172, 350, 162), (394, 109, 350, 105),
        (403, 166, 378, 149), (378, 149, 378, 68), (378, 68, 350, 47),
    ]
    for coord in vinculos:
        linha(d, *coord)
    for x, y, t in (
        (153, 163, "Registrar pedido"), (301, 163, "Gerir catálogo"),
        (153, 105, "Movimentar estoque"), (301, 105, "Simular pagamento"),
        (153, 47, "Operar preparo"), (301, 47, "Gerir contas e auditar"),
    ):
        d.add(Ellipse(x, y, 54, 15, fillColor=WHITE, strokeColor=EDGE, strokeWidth=.75))
        texto(d, x, y - 3, t, 9)
    ator(d, 35, 173, "Cliente", "canais")
    ator(d, 35, 121, "Atendente", "OPERADOR")
    ator(d, 35, 51, "Cozinha", "COZINHA")
    ator(d, 417, 172, "Gerente", "ADMIN")
    ator(d, 406, 109, "Gateway", "simulado")
    return d


def der():
    d = Drawing(WIDTH, 276)
    coord = {
        "Produto": (10, 231), "Unidade": (172, 231), "Usuário": (334, 231),
        "ProdutoUnidade": (10, 168), "Pedido": (172, 168), "Auditoria": (334, 168),
        "Estoque": (10, 105), "ItemPedido": (172, 105), "EventoPedido": (334, 105),
        "Movimentação": (10, 42), "Pagamento": (172, 42),
    }
    relacoes = [
        (64, 231, 64, 208), (190, 231, 111, 208), (226, 231, 226, 208),
        (389, 231, 389, 208), (64, 168, 64, 145), (119, 181, 172, 131),
        (226, 168, 226, 145), (278, 181, 334, 131), (64, 105, 64, 82),
        (172, 176, 144, 161), (144, 161, 144, 67), (144, 67, 119, 67), (272, 168, 297, 146),
        (297, 146, 297, 65), (297, 65, 280, 65), (437, 238, 447, 221),
        (447, 221, 447, 126), (447, 126, 442, 126),
    ]
    for coord_linha in relacoes:
        linha(d, *coord_linha)
    for x, y, t in (
        (68, 215, "1 : N"), (141, 218, "1 : N"), (232, 215, "1 : N"),
        (395, 215, "0..1 : N"), (70, 153, "1 : 1"), (147, 154, "1 : N"),
        (233, 153, "1 : N"), (311, 152, "1 : N"), (70, 91, "1 : N"),
        (132, 94, "0..1 : N"), (316, 68, "1 : 0..1"), (434, 149, "0..1 : N"),
    ):
        texto(d, x, y, t, 8.4)
    detalhes = {
        "Produto": "PK id", "Unidade": "PK id", "Usuário": "PK id; UQ email",
        "ProdutoUnidade": "PK id; FK produto, unidade", "Pedido": "PK id; FK unidade",
        "Auditoria": "PK id; FK ator e alvo",
        "Estoque": "PK id; FK/UQ vínculo", "ItemPedido": "PK id; FK pedido, vínculo",
        "EventoPedido": "PK id; FK pedido, usuário",
        "Movimentação": "PK id; FK estoque, pedido", "Pagamento": "PK id; FK/UQ pedido",
    }
    for nome, (x, y) in coord.items():
        caixa(d, x, y, 109, 40, nome, (detalhes[nome],))
    texto(d, 12, 18, "PK: chave primária   FK: chave estrangeira   UQ: unicidade   N: vários",
          8.6, "start")
    return d


def classes():
    d = Drawing(WIDTH, 310)
    linhas = [
        (162, 268, 127, 268), (306, 268, 325, 268), (224, 218, 224, 241),
        (390, 218, 390, 241), (325, 189, 306, 189), (68, 143, 68, 164),
        (292, 140, 325, 170), (390, 143, 390, 164), (162, 41, 127, 41),
    ]
    for pontos in linhas:
        seta(d, *pontos)
    for pontos in ((127, 199, 144, 199), (144, 199, 144, 249)):
        linha(d, *pontos)
    seta(d, 144, 249, 162, 249)
    for x, y, w, h, titulo, atributos in (
        (10, 241, 117, 56, "Produto", ("nome, descricao, ativo",)),
        (162, 241, 144, 56, "ProdutoUnidade", ("preco, habilitado, unidade, produto",)),
        (325, 241, 119, 56, "Unidade", ("nome, cidade, ativo",)),
        (10, 164, 117, 54, "Estoque", ("quantidadeFisica", "reservar(), baixarReserva()")),
        (162, 164, 144, 54, "ItemPedido", ("quantidade, precoUnitario", "produtoUnidade")),
        (325, 164, 119, 54, "Pedido", ("canalPedido, status, total", "itens, avancarPara()")),
        (10, 89, 117, 54, "MovimentacaoEstoque", ("tipo, quantidade", "saldo após operação")),
        (162, 89, 144, 54, "EventoPedido", ("status anterior e novo", "usuário responsável")),
        (325, 89, 119, 54, "Pagamento", ("status, valor", "resultado do gateway")),
        (10, 10, 117, 55, "Usuario", ("email, perfil, ativo", "hash de senha")),
        (162, 10, 144, 55, "Auditoria", ("responsável, alvo, ação, data",)),
    ):
        caixa(d, x, y, w, h, titulo, atributos, 9.3)
    return d


def sequencia():
    d = Drawing(WIDTH, 303)
    x = [47, 158, 285, 406]
    for p, rotulo in zip(x, ("Atendente / canal", "API / serviços", "PostgreSQL", "Gateway mock")):
        caixa(d, p - 43, 275, 86, 23, rotulo, fonte=9)
        linha(d, p, 275, p, 8, pontilhada=True)
    d.add(Rect(8, 8, 431, 108, strokeWidth=.45, strokeColor=EDGE, fillColor=None))
    linha(d, 8, 47, 439, 47, pontilhada=True)
    texto(d, 15, 104, "alt resultado concluído", 8.5, "start", BOLD)
    texto(d, 15, 35, "else indisponível", 8.5, "start", BOLD)
    for nivel, a, b, rotulo, resposta in (
        (259, 0, 1, "POST pedido", False),
        (238, 1, 2, "Bloquear e reservar", False),
        (217, 2, 1, "Confirmar transação", True),
        (196, 1, 0, "201, pendente", True),
        (175, 0, 1, "POST pagamento", False),
        (154, 1, 2, "Bloquear pedido", False),
        (133, 1, 3, "Simular e receber decisão", False),
        (91, 1, 2, "Baixar ou liberar reserva", False),
        (73, 2, 1, "Persistir resultado e eventos", True),
        (55, 1, 0, "201 com resultado", True),
        (19, 1, 0, "503, sem alteração", True),
    ):
        seta(d, x[a], nivel, x[b], nivel, resposta)
        texto(d, (x[a] + x[b]) / 2, nivel + 4, rotulo, 8.6)
    return d


DIAGRAMAS = {
    "flowchart": ("Casos de uso e atores", casos),
    "erDiagram": ("Diagrama entidade-relacionamento", der),
    "classDiagram": ("Classes do domínio", classes),
    "sequenceDiagram": ("Sequência de pedido e pagamento", sequencia),
}

QUADROS = {
    "Requisito funcional": "Requisitos funcionais",
    "Requisito não funcional": "Requisitos não funcionais",
    "Cenário alternativo": "Cenários alternativos do pedido",
    "Operações": "Resumo dos contratos HTTP",
    "Perfil ou ator": "Papéis e permissões",
    "Dado pessoal ou vínculo": "Tratamento de dados e retenção",
    "Endpoint e pré-condição": "Matriz de cobertura de testes",
}


def tabela(linhas, estilos_):
    cab = linhas[0]
    if len(cab) == 4:
        larguras = [.075, .36, .245, .32]
    elif cab[0] == "ID":
        larguras = [.11, .31, .58]
    elif len(cab) == 3:
        larguras = [.36, .37, .27]
    else:
        larguras = [.37, .63]
    celulas = [
        [Paragraph(f"<b>{inline(celula)}</b>" if i == 0 else inline(celula), estilos_["cell"])
         for celula in linha_]
        for i, linha_ in enumerate(linhas)
    ]
    tabela_ = LongTable(celulas, colWidths=[WIDTH * p for p in larguras],
                        hAlign="LEFT", repeatRows=1)
    tabela_.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), PALE),
        ("LINEABOVE", (0, 0), (-1, 0), .75, EDGE),
        ("LINEBELOW", (0, 0), (-1, 0), .75, EDGE),
        ("LINEBELOW", (0, 1), (-1, -1), .3, colors.HexColor("#BBBBBB")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    return tabela_


def conteudo(texto_md, estilos_):
    linhas = iter(texto_md.splitlines())
    story = []
    paragrafo = []
    tabela_ = []
    ha_secao = False
    referencias = False
    figura = 0
    quadro = 0
    indice = 0

    def fechar_paragrafo():
        if paragrafo:
            estilo = "reference" if referencias else "body"
            story.append(Paragraph(inline(" ".join(paragrafo)), estilos_[estilo]))
            paragrafo.clear()

    def fechar_tabela():
        nonlocal quadro
        if tabela_:
            cab = tabela_[0]
            chave = cab[1] if cab[0] == "ID" else cab[0]
            quadro += 1
            titulo = QUADROS.get(chave, chave)
            story.append(Paragraph(f"Quadro {quadro} - {escape(titulo)}", estilos_["table_caption"]))
            story.append(tabela(tabela_, estilos_))
            story.append(Paragraph("Fonte: especificação do projeto (2026).", estilos_["caption"]))
            story.append(Spacer(1, 7))
            tabela_.clear()

    for s in linhas:
        if s.startswith("|"):
            fechar_paragrafo()
            celulas = [valor.strip() for valor in s.strip("|").split("|")]
            if all(re.fullmatch(r":?-{3,}:?", valor) for valor in celulas):
                continue
            tabela_.append(celulas)
            continue
        fechar_tabela()
        if not s:
            fechar_paragrafo()
        elif s.startswith("# "):
            fechar_paragrafo()
        elif s.startswith(("## ", "### ")):
            fechar_paragrafo()
            nivel = 0 if s.startswith("## ") else 1
            titulo = s.split(" ", 1)[1]
            if nivel == 0:
                if titulo.startswith(("REFERÊNCIAS", "APÊNDICE")):
                    story.append(PageBreak())
                elif ha_secao:
                    story.append(Spacer(1, 17))
                ha_secao = True
                referencias = titulo == "REFERÊNCIAS"
            p = Paragraph(inline(titulo), estilos_["main" if nivel == 0 else "sub"])
            indice += 1
            p.sumario = (nivel, titulo, f"secao-{indice}")
            story.append(p)
        elif s.startswith("```"):
            fechar_paragrafo()
            linguagem = s[3:]
            bloco = []
            for linha_ in linhas:
                if linha_.startswith("```"):
                    break
                bloco.append(linha_)
            if linguagem == "mermaid":
                tipo = bloco[0].split()[0]
                titulo, desenhar = DIAGRAMAS[tipo]
                figura += 1
                cabecalho = []
                if story and getattr(story[-1], "sumario", None):
                    cabecalho.append(story.pop())
                story.append(KeepTogether(cabecalho + [
                    Spacer(1, 8),
                    Paragraph(f"Figura {figura} - {titulo}", estilos_["caption"]),
                    desenhar(),
                    Paragraph("Fonte: elaboração para o projeto com assistência de IA (2026).", estilos_["caption"]),
                    Spacer(1, 8),
                ]))
            else:
                story.append(KeepTogether([Preformatted("\n".join(bloco), estilos_["code"])]))
        else:
            paragrafo.append(s)
    fechar_paragrafo()
    fechar_tabela()
    return story


def pretextuais(args, estilos_):
    centro = estilos_["center"]
    titulo = estilos_["title"]
    nome = escape(args.autor.upper())
    instituicao = escape(args.instituicao.upper())
    curso = escape(args.curso.upper())
    cidade = escape(args.cidade)
    disciplina = escape(args.disciplina)
    ru = escape(args.ru)
    ano = escape(args.ano)
    subtitulo = "API para gestão multicanal de pedidos, estoque e pagamentos"
    sumario = TableOfContents(dotsMinLevel=0)
    sumario.levelStyles = [
        ParagraphStyle("Sumário 1", fontName=BOLD, fontSize=11.5, leading=16,
                       leftIndent=0, firstLineIndent=0, spaceBefore=2),
        ParagraphStyle("Sumário 2", fontName=FONT, fontSize=11, leading=15,
                       leftIndent=13, firstLineIndent=0, spaceBefore=1),
    ]
    return [
        Paragraph(instituicao, titulo),
        Paragraph(curso, centro),
        Spacer(1, 48),
        Paragraph(nome, centro),
        Paragraph(f"RU {ru}", centro),
        Spacer(1, 137),
        Paragraph("RAÍZES DO NORDESTE", titulo),
        Paragraph(subtitulo, centro),
        Spacer(1, 245),
        Paragraph(cidade, centro),
        Paragraph(ano, centro),
        PageBreak(),
        Paragraph(nome, centro),
        Paragraph(f"RU {ru}", centro),
        Spacer(1, 145),
        Paragraph("RAÍZES DO NORDESTE", titulo),
        Paragraph(subtitulo, centro),
        Spacer(1, 52),
        Paragraph(
            f"Trabalho apresentado ao curso de {escape(args.curso)} da {instituicao} "
            f"como atividade prática da disciplina {disciplina}.",
            estilos_["nature"],
        ),
        Spacer(1, 236),
        Paragraph(cidade, centro),
        Paragraph(ano, centro),
        PageBreak(),
        Paragraph("RESUMO", titulo),
        Spacer(1, 12),
        Paragraph(
            "Este projeto desenvolve uma API REST para a rede Raízes do Nordeste, com cardápio "
            "e estoque por unidade, vendas identificadas por canal e acompanhamento do pedido. "
            "A implementação usa Java, Spring Boot e PostgreSQL. O fluxo reserva estoque em "
            "transação, simula resultados de pagamento e registra mudanças de estado e "
            "movimentações. A autenticação usa JWT e a autorização restringe as operações por "
            "perfil. A suíte mais recente passou com 26 testes JUnit em H2. A coleção passou com 48 cenários HTTP em PostgreSQL 16. "
            "A proposta de fidelidade define crédito, resgate e campanhas com adesão facultativa. "
            "O pagamento permanece simulado, e a implantação em produção exige validar controles "
            "operacionais e de proteção de dados.", estilos_["reference"],
        ),
        Paragraph("<b>Palavras-chave:</b> API REST. Pedido multicanal. Estoque. Pagamento simulado.",
                  estilos_["reference"]),
        PageBreak(),
        Paragraph("ABSTRACT", titulo),
        Spacer(1, 12),
        Paragraph(
            "This project implements a REST API for the Raízes do Nordeste restaurant chain. "
            "It provides branch-specific menus and inventories, captures the sales channel, and "
            "tracks orders. The implementation uses Java, Spring Boot, and PostgreSQL. The "
            "workflow reserves stock in a transaction, simulates payment results, and records "
            "status changes and stock movements. Authentication uses JWT, and authorization "
            "applies role-based rules. The latest suite passed 26 JUnit tests on H2. The collection passed 48 HTTP "
            "scenarios against PostgreSQL 16. The proposed loyalty program specifies earning, redemption, "
            "and campaigns with voluntary participation. Payment remains simulated, and a "
            "production deployment requires operational and privacy review.", estilos_["reference"],
        ),
        Paragraph("<b>Keywords:</b> REST API. Multichannel orders. Inventory. Simulated payments.",
                  estilos_["reference"]),
        PageBreak(),
        Paragraph("SUMÁRIO", titulo),
        Spacer(1, 16),
        sumario,
        PageBreak(),
    ]


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--autor", default="[Nome do autor]")
    p.add_argument("--ru", default="[RU do autor]")
    p.add_argument("--instituicao", default="[Instituição]")
    p.add_argument("--curso", default="[Curso]")
    p.add_argument("--disciplina", default="[Disciplina]")
    p.add_argument("--cidade", default="[Cidade]")
    p.add_argument("--ano", default="2026")
    p.add_argument("--saida", type=Path, required=True)
    p.add_argument("--fontes", type=Path, default=Path("C:/Windows/Fonts"))
    args = p.parse_args()
    fontes(args.fontes)
    estilo = estilos()
    args.saida.parent.mkdir(parents=True, exist_ok=True)
    doc = Relatorio(str(args.saida), pagesize=A4, leftMargin=3 * cm, rightMargin=2 * cm,
                    topMargin=3 * cm, bottomMargin=2 * cm, title="Raízes do Nordeste",
                    author=args.autor, subject="Projeto acadêmico de desenvolvimento Back-End")
    doc.addPageTemplates(PageTemplate(id="A4", onPageEnd=numero_pagina, frames=[
        Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height,
              leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0),
    ]))
    md = (ROOT / "docs/especificacao.md").read_text(encoding="utf-8-sig")
    doc.multiBuild(pretextuais(args, estilo) + conteudo(md, estilo))


if __name__ == "__main__":
    main()
