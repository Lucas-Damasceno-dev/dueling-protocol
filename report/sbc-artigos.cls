%%
%% This is file `sbc-artigos.cls',
%% generated with the docstrip utility.
%%
%% The original source files were:
%%
%% sbc.dtx  (with options: `artigos')
%% 
%% This is a generated file.
%% 
%% Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 Sociedade Brasileira de Computacao
%% -----------------------------------------------------------------------
%% 
%% This work may be distributed and/or modified under the
%% conditions of the LaTeX Project Public License, either version 1.3
%% of this license or (at your option) any later version.
%% The latest version of this license is in
%%   http://www.latex-project.org/lppl.txt
%% and version 1.3 or later is part of all distributions of LaTeX
%% version 2003/12/01 or later.
%% 
%% This work has the LPPL maintenance status "maintained".
%% 
%% The Current Maintainer of this work is Ricardo Nakamura.
%% 
%% This work consists of the files sbc.dtx and sbc.ins
%% and the derived files sbc-artigos.cls, sbc-template.tex,
%% sbc-template-en.tex, sbc-biblio.bib, sbc-abnt.bst,
%% sbc-alphabetic.bst.
%% 
\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{sbc-artigos}[2015/01/26 v2.3 SBC article class]
\newif\if@brazil
\@braziltrue
\DeclareOption{english}{%
  \@brazilfalse%
}
\DeclareOption*{\PassOptionsToClass{\CurrentOption}{article}}
\ProcessOptions
\LoadClass{article}
\RequirePackage{ifthen}
\RequirePackage[latin1]{inputenc}
\RequirePackage{graphicx}
\RequirePackage{color}
\RequirePackage{url}
\if@brazil
  \RequirePackage[brazil]{babel}
\fi
\def\sbc{\textrm{SBC}}
\def\titulo#1{\title{#1}}
\def\autor#1#2{\author{#1 \\ #2}}
\def\address#1{\date{#1}}
\def\email#1{\texttt{#1}}
\def\instituicao#1#2{\author{#1 \\ #2}}
\def\artigocurto#1{}
\def\imprimirartigo#1{}
\def\volume#1{}
\def\numero#1{}
\def\mes#1{}
\def\ano#1{}
\def\doi#1{}
\def\issn#1{}
\def\conference#1{}
\def\confshortname#1{}
\def\confdate#1{}
\def\conflocation#1{}
\def\confsponsors#1{}
\def\editor#1{}
\def\resumo#1{\begin{abstract}#1\end{abstract}}
\def\abstract#1{\begin{abstract}#1\end{abstract}}
\def\palavraschave#1{\textbf{\ifthenelse{\equal{\@brazil}{true}}{Palavras-chave:}{Keywords:}} #1}
\def\keywords#1{\textbf{Keywords:} #1}
\def\maketitle{%
  \begingroup
    \newpage
    \null
    \vskip 2em%
    \begin{center}%
      \let \footnote \thanks
      {\LARGE \@title \par}%
      \vskip 1.5em%
      {\large
       \lineskip .5em%
       \begin{tabular}[t]{c}%
         \@author
       \end{tabular}\par}%
      \vskip 1em%
      {\large \@date}%
    \end{center}%
    \par
    \vskip 1.5em
  \endgroup
}
\def\imprimirtitulo{%
  \begingroup
    \newpage
    \null
    \vskip 2em%
    \begin{center}%
      \let \footnote \thanks
      {\LARGE \@title \par}%
      \vskip 1.5em%
      {\large
       \lineskip .5em%
       \begin{tabular}[t]{c}%
         \@author
       \end{tabular}\par}%
      \vskip 1em%
      {\large \@date}%
    \end{center}%
    \par
    \vskip 1.5em
  \endgroup
}
\renewenvironment{abstract}{%
  \small
  \begin{center}%
    \bfseries \abstractname
  \end{center}%
  \quotation
}
\def\section{\@startsection {section}{1}{\z@}%
                                   {-3.5ex \@plus -1ex \@minus -.2ex}%
                                   {2.3ex \@plus.2ex}%
                                   {\normalfont\Large\bfseries}}
\def\subsection{\@startsection{subsection}{2}{\z@}%
                                     {-3.25ex\@plus -1ex \@minus -.2ex}%
                                     {1.5ex \@plus .2ex}%
                                     {\normalfont\large\bfseries}}
\def\subsubsection{\@startsection{subsubsection}{3}{\z@}%
                                     {-3.25ex\@plus -1ex \@minus -.2ex}%
                                     {1.5ex \@plus .2ex}%
                                     {\normalfont\normalsize\bfseries}}
\def\paragraph{\@startsection{paragraph}{4}{\z@}%
                                    {3.25ex \@plus1ex \@minus.2ex}%
                                    {-1em}%
                                    {\normalfont\normalsize\bfseries}}
\def\subparagraph{\@startsection{subparagraph}{5}{\parindent}%
                                       {3.25ex \@plus1ex \@minus .2ex}%
                                       {-1em}%
                                      {\normalfont\normalsize\bfseries}}
\endinput
%%
%% End of file `sbc-artigos.cls'.