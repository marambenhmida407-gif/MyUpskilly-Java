<?php
namespace App\Entity;
use App\Repository\PatologieRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: PatologieRepository::class)]
#[ORM\Table(name: 'patologie')]
class Patologie
{
    #[ORM\Id] #[ORM\GeneratedValue] #[ORM\Column]
    private ?int $id = null;
    #[ORM\Column(name: 'user_id')]           private ?int $userId = null;
    #[ORM\Column(length: 255)]               private ?string $nom = null;
    #[ORM\Column(type: 'text', nullable: true)] private ?string $description = null;
    #[ORM\Column(length: 100, nullable: true)] private ?string $type = null;
    #[ORM\Column(length: 50, nullable: true)] private ?string $gravite = null;

    private ?string $userNom = null;

    public function getId(): ?int { return $this->id; }
    public function getUserId(): ?int { return $this->userId; }
    public function setUserId(int $v): static { $this->userId = $v; return $this; }
    public function getNom(): ?string { return $this->nom; }
    public function setNom(string $v): static { $this->nom = $v; return $this; }
    public function getDescription(): ?string { return $this->description; }
    public function setDescription(?string $v): static { $this->description = $v; return $this; }
    public function getType(): ?string { return $this->type; }
    public function setType(?string $v): static { $this->type = $v; return $this; }
    public function getGravite(): ?string { return $this->gravite; }
    public function setGravite(?string $v): static { $this->gravite = $v; return $this; }
    public function getUserNom(): ?string { return $this->userNom; }
    public function setUserNom(?string $v): static { $this->userNom = $v; return $this; }
}
